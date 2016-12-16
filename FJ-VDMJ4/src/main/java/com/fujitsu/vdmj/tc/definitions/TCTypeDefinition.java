/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCPatternListList;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * A class to hold a type definition.
 */
public class TCTypeDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public TCInvariantType type;
	public final TCPattern invPattern;
	public final TCExpression invExpression;
	public TCExplicitFunctionDefinition invdef;
	public boolean infinite = false;
	private TCDefinitionList composeDefinitions;

	public TCTypeDefinition(TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCInvariantType type, TCPattern invPattern, TCExpression invExpression)
	{
		super(Pass.TYPES, name.getLocation(), name, NameScope.TYPENAME);

		this.accessSpecifier = accessSpecifier;
		this.type = type;
		this.invPattern = invPattern;
		this.invExpression = invExpression;
		
		type.definitions = new TCDefinitionList(this);
		composeDefinitions = new TCDefinitionList();
	}

	@Override
	public String toString()
	{
		return accessSpecifier.ifSet(" ") +
				name.getName() + " = " + type.toDetailedString() +
				(invPattern == null ? "" :
					"\n\tinv " + invPattern + " == " + invExpression);
	}
	
	@Override
	public String kind()
	{
		return "type";
	}

	@Override
	public void implicitDefinitions(Environment base)
	{
		if (invPattern != null)
		{
    		invdef = getInvDefinition();
    		type.setInvariant(invdef);
		}
		else
		{
			invdef = null;
		}
		
		// TCType definitions of the form "A = compose B of ... end" also define the type
		// B, which can be used globally. Here, we assume all compose types are legal
		// but in the typeCheck we check whether they match any existing definitions.
		
		if (type instanceof TCNamedType)
		{
			composeDefinitions.clear();
			TCNamedType nt = (TCNamedType)type;

			for (TCType compose: nt.type.getComposeTypes())
			{
				TCRecordType rtype = (TCRecordType)compose;
				composeDefinitions.add(new TCTypeDefinition(TCAccessSpecifier.DEFAULT, rtype.name, rtype, null, null));
			}
		}
	}
	
	@Override
	public void typeResolve(Environment base)
	{
		try
		{
			infinite = false;
			type = (TCInvariantType)type.typeResolve(base, this);

			if (infinite)
			{
				report(3050, "Type '" + name + "' is infinite");
			}

			if (invdef != null)
			{
				invdef.typeResolve(base);
				invPattern.typeResolve(base);
			}
			
			composeDefinitions.typeResolve(base);
		}
		catch (TypeCheckException e)
		{
			type.unResolve();
			throw e;
		}
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		if (invdef != null)
		{
			invdef.typeCheck(base, NameScope.NAMES);
		}
		
		if (type instanceof TCNamedType)
		{
			// Rebuild the compose definitions, after we check whether they already exist
			composeDefinitions.clear();
			TCNamedType nt = (TCNamedType)type;

			for (TCType compose: TypeComparator.checkComposeTypes(nt.type, base, true))
			{
				TCRecordType rtype = (TCRecordType)compose;
				TCDefinition cdef = new TCTypeDefinition(accessSpecifier, rtype.name, rtype, null, null);
				composeDefinitions.add(cdef);
			}
		}

		// We have to do the "top level" here, rather than delegating to the types
		// because the definition pointer from these top level types just refers
		// to the definition we are checking, which is never "narrower" than itself.
		// See the narrowerThan method in TCNamedType and TCRecordType.
		
		if (type instanceof TCNamedType)
		{
			TCNamedType ntype = (TCNamedType)type;
			
			if (ntype.type.narrowerThan(accessSpecifier))
			{
				report(3321, "Type component visibility less than type's definition");
			}
		}
		else if (type instanceof TCRecordType)
		{
			TCRecordType rtype = (TCRecordType)type;
			
			for (TCField field: rtype.fields)
			{
				if (field.type.narrowerThan(accessSpecifier))
				{
					field.tagname.report(3321, "Field type visibility less than type's definition");
				}
			}
		}
	}

	@Override
	public TCType getType()
	{
		return type;
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope incState)
	{
		if (invdef != null && invdef.findName(sought, incState) != null)
		{
			return invdef;
		}

		return null;
	}

	@Override
	public TCDefinition findType(TCNameToken sought, String fromModule)
	{
		if (composeDefinitions != null)
		{
			TCDefinition d = composeDefinitions.findType(sought, fromModule);
			
			if (d != null)
			{
				return d;
			}
		}
		
		return super.findName(sought, NameScope.TYPENAME);
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		TCDefinitionList defs = new TCDefinitionList(this);
		defs.addAll(composeDefinitions);

		if (invdef != null)
		{
			defs.add(invdef);
		}

		return defs;
	}

	@Override
	public TCNameList getVariableNames()
	{
		// This is only used in VDM++ type inheritance
		return new TCNameList(name);
	}

	private TCExplicitFunctionDefinition getInvDefinition()
	{
		LexLocation loc = invPattern.location;
		TCPatternList params = new TCPatternList();
		params.add(invPattern);

		TCPatternListList parameters = new TCPatternListList();
		parameters.add(params);

		TCTypeList ptypes = new TCTypeList();

		if (type instanceof TCRecordType)
		{
			// Records are inv_R: R +> bool
			ptypes.add(new TCUnresolvedType(name));
		}
		else
		{
			// Named types are inv_T: x +> bool, for T = x
			TCNamedType nt = (TCNamedType)type;
			ptypes.add(nt.type);
		}

		TCFunctionType ftype =
			new TCFunctionType(loc, ptypes, false, new TCBooleanType(loc));

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(accessSpecifier, name.getInvName(loc),
			null, ftype, parameters, invExpression, null, null, true, null);

		def.classDefinition = classDefinition;
		ftype.definitions = new TCDefinitionList(def);
		return def;
	}

	@Override
	public boolean isRuntime()
	{
		return false;	// Though the inv definition is, of course
	}

	@Override
	public boolean isTypeDefinition()
	{
		return true;
	}
}
