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

import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCMultipleTypeBind extends TCMultipleBind
{
	private static final long serialVersionUID = 1L;
	public TCType type;

	public TCMultipleTypeBind(TCPatternList plist, TCType type)
	{
		super(plist);
		this.type = type;
	}

	@Override
	public String toString()
	{
		return plist + ":" + type;
	}

	@Override
	public TCType typeCheck(Environment base, NameScope scope)
	{
		plist.typeResolve(base);
		type = type.typeResolve(base, null);
		TCType ptype = getPossibleType();
		
		TypeComparator.checkComposeTypes(type, base, false);

		if (!TypeComparator.compatible(ptype, type))
		{
			type.report(3265, "At least one bind cannot match this type");
			type.detail2("Binds", ptype, "Type", type);
		}

		return type;
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env)
	{
		return type.getFreeVariables(env);
	}
}
