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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCStateInitExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCPatternListList;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFieldList;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold a module's state definition.
 */
public class TCStateDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCFieldList fields;
	public final TCPattern invPattern;
	public final TCExpression invExpression;
	public final TCPattern initPattern;
	public final TCExpression initExpression;

	public TCExplicitFunctionDefinition invdef = null;
	public TCExplicitFunctionDefinition initdef = null;

	public final TCDefinitionList statedefs;
	private TCRecordType recordType;
	public boolean canBeExecuted = true;
	public TCTypeList unresolved = new TCTypeList();

	public TCStateDefinition(TCNameToken name, TCFieldList fields, TCPattern invPattern,
		TCExpression invExpression, TCPattern initPattern, TCExpression initExpression)
	{
		super(Pass.TYPES, name.getLocation(), name, NameScope.STATE);

		this.fields = fields;
		this.invPattern = invPattern;
		this.invExpression = invExpression;
		this.initPattern = initPattern;
		this.initExpression = initExpression;

		statedefs = new TCDefinitionList();

		recordType = new TCRecordType(name, fields, false);
		TCLocalDefinition recordDefinition = null;

		recordDefinition = new TCLocalDefinition(location, name, recordType, NameScope.STATE);
		recordDefinition.markUsed();	// Can't be exported anyway
		statedefs.add(recordDefinition);

		recordDefinition = new TCLocalDefinition(location, name.getOldName(), recordType, NameScope.STATE);
		recordDefinition.markUsed();	// Can't be exported anyway
		statedefs.add(recordDefinition);

		for (TCField f: fields)
		{
			unresolved.addAll(f.type.unresolvedTypes());
			
			statedefs.add(new TCLocalDefinition(f.tagname.getLocation(),
				f.tagname, f.type, NameScope.STATE));

			TCLocalDefinition ld = new TCLocalDefinition(f.tagname.getLocation(),
				f.tagname.getOldName(), f.type, NameScope.OLDSTATE);

			ld.markUsed();		// Else we moan about unused ~x names
			statedefs.add(ld);
		}
	}

	@Override
	public String toString()
	{
		return "state " + name + " of\n" + Utils.listToString(fields, "\n") +
			(invPattern == null ? "" : "\n\tinv " + invPattern + " == " + invExpression) +
    		(initPattern == null ? "" : "\n\tinit " + initPattern + " == " + initExpression) +
    		"\nend";
	}
	
	@Override
	public String kind()
	{
		return "state";
	}

	@Override
	public void implicitDefinitions(Environment base)
	{
		if (invPattern != null)
		{
			invdef = getInvDefinition();
		}

		if (initPattern != null)
		{
			initdef = getInitDefinition();
		}
	}

	@Override
	public void typeResolve(Environment env)
	{
		for (TCField f: fields)
		{
			try
			{
				f.typeResolve(env);
			}
			catch (TypeCheckException e)
			{
				f.unResolve();
				throw e;
			}
		}

		recordType = (TCRecordType) recordType.typeResolve(env);

		if (invPattern != null)
		{
			invdef.typeResolve(env);
			recordType.setInvariant(invdef);
		}

		if (initPattern != null)
		{
			initdef.typeResolve(env);
		}
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		if (annotations != null) annotations.tcBefore(this, base, scope);

		if (pass == Pass.TYPES)
		{
			if (base.findStateDefinition() != this)
			{
				report(3047, "Only one state definition allowed per module");
				return;
			}

			for (TCField field: recordType.fields)
			{
				TypeComparator.checkComposeTypes(field.type, base, false);
			}

			TypeComparator.checkImports(base, unresolved, location.module);
			statedefs.typeCheck(base, scope);
			pass = Pass.DEFS;
		}
		else
		{
			if (invdef != null)
			{
				invdef.typeCheck(base, scope);
			}

			if (initdef != null)
			{
				initdef.typeCheck(base, scope);
			}
		}

		if (annotations != null) annotations.tcAfter(this, recordType, base, scope);
	}

	@Override
	public TCDefinition findType(TCNameToken sought, String fromModule)
	{
		if (super.findName(sought, NameScope.STATE) != null)
		{
			return this;
		}

		return null;
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		if (scope.matches(NameScope.NAMES))
		{
    		if (invdef != null && invdef.findName(sought, scope) != null)
    		{
    			return invdef;
    		}

    		if (initdef != null && initdef.findName(sought, scope) != null)
    		{
    			return initdef;
    		}
		}

		for (TCDefinition d: statedefs)
		{
			TCDefinition def = d.findName(sought, scope);

			if (def != null)
			{
				return def;
			}
		}

		return null;
	}

	@Override
	public TCType getType()
	{
		return recordType;
	}

	@Override
	public void unusedCheck()
	{
		statedefs.unusedCheck();
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return statedefs;
	}

	private TCExplicitFunctionDefinition getInvDefinition()
	{
		LexLocation loc = invPattern.location;
		TCPatternList params = new TCPatternList();
		params.add(invPattern);

		TCPatternListList parameters = new TCPatternListList();
		parameters.add(params);

		TCTypeList ptypes = new TCTypeList();
		ptypes.add(new TCUnresolvedType(name));
		TCFunctionType ftype = new TCFunctionType(loc, ptypes, false, new TCBooleanType(loc));

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, TCAccessSpecifier.DEFAULT,
			name.getInvName(invPattern.location), null, ftype, parameters, invExpression, null, null, true, null);

		ftype.definitions = new TCDefinitionList(def);
		return def;
	}

	private TCExplicitFunctionDefinition getInitDefinition()
	{
		LexLocation loc = initPattern.location;
		TCPatternList params = new TCPatternList();
		params.add(initPattern);

		TCPatternListList parameters = new TCPatternListList();
		parameters.add(params);

		TCTypeList ptypes = new TCTypeList();
		ptypes.add(new TCUnresolvedType(name));
		TCFunctionType ftype = new TCFunctionType(loc, ptypes, true, new TCBooleanType(loc));

		TCExpression body = new TCStateInitExpression(this);

		TCExplicitFunctionDefinition def = new TCExplicitFunctionDefinition(null, TCAccessSpecifier.DEFAULT,
			name.getInitName(initPattern.location), null, ftype, parameters, body, null, null, false, null);

		ftype.definitions = new TCDefinitionList(def);
		return def;
	}
	
	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseStateDefinition(this, arg);
	}
}
