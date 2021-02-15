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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
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
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * A class to hold a type definition.
 */
public class TCTypeDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public TCInvariantType type;
	public final TCTypeList unresolved;
	public final TCPattern invPattern;
	public final TCExpression invExpression;
	public final TCPattern eqPattern1;
	public final TCPattern eqPattern2;
	public final TCExpression eqExpression;
	public final TCPattern ordPattern1;
	public final TCPattern ordPattern2;
	public final TCExpression ordExpression;
	
	public TCExplicitFunctionDefinition invdef;
	public TCExplicitFunctionDefinition eqdef;
	public TCExplicitFunctionDefinition orddef;
	public TCExplicitFunctionDefinition mindef;
	public TCExplicitFunctionDefinition maxdef;
	
	public boolean infinite = false;
	private TCDefinitionList composeDefinitions;

	public TCTypeDefinition(TCAnnotationList annotations, TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCInvariantType type, TCPattern invPattern, TCExpression invExpression,
		TCPattern eqPattern1, TCPattern eqPattern2, TCExpression eqExpression,
		TCPattern ordPattern1, TCPattern ordPattern2, TCExpression ordExpression)
	{
		super(Pass.TYPES, name.getLocation(), name, NameScope.TYPENAME);

		this.annotations = annotations;
		this.accessSpecifier = accessSpecifier;
		this.type = type;
		this.unresolved = type.unresolvedTypes();
		this.invPattern = invPattern;
		this.invExpression = invExpression;
		this.eqPattern1 = eqPattern1;
		this.eqPattern2 = eqPattern2;
		this.eqExpression = eqExpression;
		this.ordPattern1 = ordPattern1;
		this.ordPattern2 = ordPattern2;
		this.ordExpression = ordExpression;
		
		type.definitions = new TCDefinitionList(this);
		composeDefinitions = new TCDefinitionList();
	}

	@Override
	public String toString()
	{
		return accessSpecifier.ifSet(" ") +
				name.getName() + " = " + type.toDetailedString() +
				(invPattern == null ? "" :
					"\n\tinv " + invPattern + " == " + invExpression) +
        		(eqPattern1 == null ? "" :
        			"\n\teq " + eqPattern1 + " = " + eqPattern2 + " == " + eqExpression) +
        		(ordPattern1 == null ? "" :
        			"\n\tord " + ordPattern1 + " < " + ordPattern2 + " == " + ordExpression);
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
		
		if (eqPattern1 != null)
		{
    		eqdef = getEqOrdDefinition(eqPattern1, eqPattern2,
    			eqExpression, name.getEqName(eqExpression.location));
    		type.setEquality(eqdef);
		}
		else
		{
			eqdef = null;
		}
		
		if (ordPattern1 != null)
		{
    		orddef = getEqOrdDefinition(ordPattern1, ordPattern2,
    			ordExpression, name.getOrdName(ordExpression.location));
    		type.setOrder(orddef);
    		
    		mindef = getMinMaxDefinition(true, name.getMinName(ordExpression.location));
    		maxdef = getMinMaxDefinition(false, name.getMaxName(ordExpression.location));
		}
		else
		{
			orddef = null;
			mindef = null;
			maxdef = null;
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
				composeDefinitions.add(new TCTypeDefinition(null, TCAccessSpecifier.DEFAULT,
						rtype.name, rtype, null, null, null, null, null, null, null, null));
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
			
			if (eqdef != null)
			{
				eqdef.typeResolve(base);
				eqPattern1.typeResolve(base);
				eqPattern2.typeResolve(base);
			}
			
			if (orddef != null)
			{
				orddef.typeResolve(base);
				ordPattern1.typeResolve(base);
				ordPattern2.typeResolve(base);
			}
			
			if (mindef != null)
			{
				mindef.typeResolve(base);
			}
			
			if (maxdef != null)
			{
				maxdef.typeResolve(base);
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
		if (annotations != null) annotations.tcBefore(this, base, scope);

		// We have to perform the type check in two passes because the invariants can depend on
		// the types of "values" that have not been set yet. Initially, pass == TYPES.
		
		if (pass == Pass.DEFS)		// Set below
		{
			if (invdef != null)
			{
				invdef.typeCheck(base, NameScope.NAMES);
			}

			if (eqdef != null)
			{
				eqdef.typeCheck(base, NameScope.NAMES);
			}

			if (orddef != null)
			{
				orddef.typeCheck(base, NameScope.NAMES);
			}

			// Suppress any TC errors around min/max as they can only add confusion
			TypeChecker.suspend(true);

			if (mindef != null)
			{
				mindef.typeCheck(base, NameScope.NAMES);
			}

			if (maxdef != null)
			{
				maxdef.typeCheck(base, NameScope.NAMES);
			}

			TypeChecker.suspend(false);
		}
		else
		{
			pass = Pass.DEFS;		// Come back later for the invariant functions
			
			if (type.isUnion(location))
			{
				TCUnionType ut = type.getUnion();
				
				for (TCType t: ut.types)
				{
					if (orddef != null && t instanceof TCInvariantType)
					{
						TCInvariantType it = (TCInvariantType) t;
						
						if (it.orddef != null)
						{
							warning(5019, "Order of union member " + t + " will be overridden");
						}
					}
					
					if (eqdef != null && t.isEq(location))
					{
						warning(5020, "Equality of union member " + t + " will be overridden");
					}
				}
			}

			TypeComparator.checkImports(base, unresolved, location.module);

			if (type instanceof TCNamedType)
			{
				// Rebuild the compose definitions, after we check whether they already exist
				composeDefinitions.clear();
				TCNamedType nt = (TCNamedType)type;
	
				for (TCType compose: TypeComparator.checkComposeTypes(nt.type, base, true))
				{
					TCRecordType rtype = (TCRecordType)compose;
					TCDefinition cdef = new TCTypeDefinition(null, accessSpecifier,
							rtype.name, rtype, null, null, null, null, null, null, null, null);
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
					
					if (field.equalityAbstraction && eqdef != null)
					{
						field.tagname.warning(5018, "Field has ':-' for type with eq definition");
					}
				}
			}
		}

		if (annotations != null) annotations.tcAfter(this, type, base, scope);
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

		if (eqdef != null && eqdef.findName(sought, incState) != null)
		{
			return eqdef;
		}

		if (orddef != null && orddef.findName(sought, incState) != null)
		{
			return orddef;
		}

		if (mindef != null && mindef.findName(sought, incState) != null)
		{
			return mindef;
		}

		if (maxdef != null && maxdef.findName(sought, incState) != null)
		{
			return maxdef;
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

		if (eqdef != null)
		{
			defs.add(eqdef);
		}

		if (orddef != null)
		{
			defs.add(orddef);
		}

		if (mindef != null)
		{
			defs.add(mindef);
		}

		if (maxdef != null)
		{
			defs.add(maxdef);
		}

		return defs;
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

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier,
			name.getInvName(loc), null, ftype, parameters, invExpression, null, null, true, null);

		def.classDefinition = classDefinition;
		ftype.definitions = new TCDefinitionList(def);
		return def;
	}

	private TCExplicitFunctionDefinition getEqOrdDefinition(TCPattern p1, TCPattern p2, TCExpression exp, TCNameToken fname)
	{
		LexLocation loc = p1.location;
		TCPatternList params = new TCPatternList();
		params.add(p1);
		params.add(p2);

		TCPatternListList parameters = new TCPatternListList();
		parameters.add(params);

		TCTypeList ptypes = new TCTypeList();
		if (type instanceof TCRecordType)
		{
			// Records are xxx_R: R * R +> bool
			ptypes.add(new TCUnresolvedType(name));
			ptypes.add(new TCUnresolvedType(name));
		}
		else
		{
			// Named types are xxx_T: X * X +> bool, for T = X
			TCNamedType nt = (TCNamedType)type;
			ptypes.add(nt.type);
			ptypes.add(nt.type);
		}

		TCFunctionType ftype =
			new TCFunctionType(loc, ptypes, false, new TCBooleanType(loc));

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier,
			fname, null, ftype, parameters, exp, null, null, false, null);

		def.classDefinition = classDefinition;
		ftype.definitions = new TCDefinitionList(def);
		return def;
	}

	private TCExplicitFunctionDefinition getMinMaxDefinition(boolean isMin, TCNameToken fname)
	{
		LexLocation loc = fname.getLocation();
		TCPatternList params = new TCPatternList();
		params.add(new TCIdentifierPattern(new TCNameToken(loc, loc.module, "a")));
		params.add(new TCIdentifierPattern(new TCNameToken(loc, loc.module, "b")));

		TCPatternListList parameters = new TCPatternListList();
		parameters.add(params);

		TCTypeList ptypes = new TCTypeList();
		ptypes.add(new TCUnresolvedType(name));
		ptypes.add(new TCUnresolvedType(name));

		// min_T: T * T +> T, max_T: T * T +> T
		TCFunctionType ftype = new TCFunctionType(loc, ptypes, false, new TCUnresolvedType(name));
		TCExpression body = null;
		
		try
		{
			if (isMin)
			{
				body = parse(String.format("if a < b or a = b then a else b"));
			}
			else
			{
				body = parse(String.format("if a < b or a = b then b else a"));
			}
		}
		catch (Exception e)
		{
			// Never reached
		}

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, accessSpecifier,
			fname, null, ftype, parameters, body, null, null, false, null);

		def.classDefinition = classDefinition;
		ftype.definitions = new TCDefinitionList(def);
		return def;
	}
	
	private TCExpression parse(String body) throws Exception
	{
		LexTokenReader ltr = new LexTokenReader(body, Dialect.VDM_SL);
		ExpressionReader er = new ExpressionReader(ltr);
		er.setCurrentModule(name.getModule());
		ASTExpression ast = er.readExpression();
		return ClassMapper.getInstance(TCNode.MAPPINGS).convert(ast);
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

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTypeDefinition(this, arg);
	}
}
