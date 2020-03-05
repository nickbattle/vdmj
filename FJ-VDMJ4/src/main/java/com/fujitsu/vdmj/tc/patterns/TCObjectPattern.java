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

package com.fujitsu.vdmj.tc.patterns;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.util.Utils;

public class TCObjectPattern extends TCPattern
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken classname;
	public final TCNamePatternPairList fieldlist;
	public TCType type;

	public TCObjectPattern(LexLocation location, TCNameToken classname, TCNamePatternPairList fieldlist)
	{
		super(location);
		this.classname = classname;
		this.fieldlist = fieldlist;
		this.type = new TCUnresolvedType(classname);
	}

	@Override
	public String toString()
	{
		return "obj_" + type + "(" + Utils.listToString(fieldlist) + ")";
	}

	@Override
	public void unResolve()
	{
		type.unResolve();
		resolved = false;
	}

	@Override
	public void typeResolve(Environment env)
	{
		if (resolved) return; else { resolved = true; }

		try
		{
			fieldlist.typeResolve(env);
			type = type.typeResolve(env, null);

			if (!type.isClass(env))
			{
				report(3331, "obj_ expression is not an object type");
				detail("Type", type);
			}
			else
			{
				typeCheck(env);		// Note checked from resolve for simplicity
			}
		}
		catch (TypeCheckException e)
		{
			unResolve();
			throw e;
		}
	}

	private void typeCheck(Environment base)
	{
		// Check whether the field access is permitted from here.
		TCClassType cls = type.getClassType(base);

		for (TCNamePatternPair npp: fieldlist)
		{
			TCDefinition fdef = cls.findName(npp.name, NameScope.STATE);

			if (fdef == null)
			{
				npp.name.report(3091, "Unknown member " + npp.name + " of class " + cls.name.getName());
			}
			else if (!TCClassDefinition.isAccessible(base, fdef, false))
			{
				npp.name.report(3092, "Inaccessible member " + npp.name + " of class " + cls.name.getName());
			}
		}

		if (base.isFunctional())
		{
			report(3332, "Object pattern cannot be used from a function");
		}
	}

	@Override
	public <R, S> R apply(TCPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseObjectPattern(this, arg);
	}
}
