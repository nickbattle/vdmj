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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCInstanceVariableDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.typechecker.TypeComparator;
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
	public TCDefinitionList getAllDefinitions(TCType exptype, NameScope scope)
	{
		TCDefinitionList defs = new TCDefinitionList();
		TCClassType pattype = type.getClassType(null);
		TCClassType expctype = exptype.getClassType(null);

		if (expctype == null || !TypeComparator.isSubType(pattype, expctype))
		{
			report(3333, "Matching expression is not a compatible object type");
			detail2("Pattern type", type, "Expression type", exptype);
			return defs;
		}
		
		TCDefinitionList members = pattype.classdef.getDefinitions();

		for (TCNamePatternPair npp: fieldlist)
		{
			TCDefinition d = members.findName(npp.name, NameScope.STATE);	// NB. state lookup
			
			if (d != null)
			{
				d = d.deref();
			}
			
			if (d instanceof TCInstanceVariableDefinition)
			{
				defs.addAll(npp.pattern.getAllDefinitions(d.getType(), scope));
			}
			else
			{
				report(3334, npp.name.getName() + " is not a matchable field of class " + pattype);
			}
		}

		return defs;
	}

	@Override
	public TCNameList getAllVariableNames()
	{
		TCNameList list = new TCNameList();

		for (TCNamePatternPair npp: fieldlist)
		{
			list.addAll(npp.pattern.getAllVariableNames());
		}

		return list;
	}

	@Override
	public TCType getPossibleType()
	{
		return type;
	}

	@Override
	public List<TCIdentifierPattern> findIdentifiers()
	{
		List<TCIdentifierPattern> list = new Vector<TCIdentifierPattern>();

		for (TCNamePatternPair npp: fieldlist)
		{
			list.addAll(npp.pattern.findIdentifiers());
		}

		return list;
	}

	public boolean alwaysMatches()
	{
		return fieldlist.alwaysMatches();
	}
}
