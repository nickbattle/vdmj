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
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;

public class TCPerSyncDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken opname;
	public final TCExpression guard;

	public TCPerSyncDefinition(TCAnnotationList annotations, LexLocation location, TCNameToken opname,
		TCExpression guard)
	{
		super(Pass.DEFS, location, opname.getPerName(location), NameScope.GLOBAL);
		this.annotations = annotations;
		this.opname = opname;
		this.guard = guard;
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return new TCDefinitionList(this);
	}

	@Override
	public TCType getType()
	{
		return new TCBooleanType(location);
	}

	@Override
	public TCNameList getVariableNames()
	{
		return new TCNameList();
	}

	@Override
	public String toString()
	{
		return "per " + opname + " => " + guard;
	}
	
	@Override
	public String kind()
	{
		return "sync";
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		return null;
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		if (annotations != null) annotations.typeCheck(this, base, scope);

		TCClassDefinition classdef = base.findClassDefinition();
		int opfound = 0;
		int perfound = 0;
		Boolean isStatic = null;

		for (TCDefinition def: classdef.getDefinitions())
		{
			if (def.name != null && def.name.matches(opname))
			{
				opfound++;

				if (!def.isCallableOperation())
				{
					opname.report(3042, opname + " is not an explicit operation");
				}
				
				if (isStatic != null && isStatic != def.isStatic())
				{
					opname.report(3323, "Overloaded operation cannot mix static and non-static");
				}
				
				if (def.isPure())
				{
					opname.report(3340, "Pure operation cannot have permission predicate");
				}
				
				isStatic = def.isStatic();
			}

			if (def instanceof TCPerSyncDefinition)
			{
				TCPerSyncDefinition psd = (TCPerSyncDefinition)def;

				if (psd.opname.equals(opname))
				{
					perfound++;
				}
			}
		}

		if (opfound == 0)
		{
			opname.report(3043, opname + " is not in scope");
		}
		else if (opfound > 1)
		{
			opname.warning(5003, "Permission guard of overloaded operation");
		}

		if (perfound != 1)
		{
			opname.report(3044, "Duplicate permission guard found for " + opname);
		}

		if (opname.getName().equals(classdef.name.getName()))
		{
			opname.report(3045, "Cannot put guard on a constructor");
		}

		FlatCheckedEnvironment local = new FlatCheckedEnvironment(this, base, scope);
		local.setEnclosingDefinition(this);	// Prevent op calls
		local.setFunctional(true);
		
		if (isStatic != null)
		{
			local.setStatic(isStatic);
		}
		
		TCType rt = guard.typeCheck(local, null, NameScope.NAMESANDSTATE, new TCBooleanType(location));

		if (!rt.isType(TCBooleanType.class, location))
		{
			guard.report(3046, "Guard is not a boolean expression");
		}
	}

	public TCExpression getExpression()
	{
		return guard;
	}
}
