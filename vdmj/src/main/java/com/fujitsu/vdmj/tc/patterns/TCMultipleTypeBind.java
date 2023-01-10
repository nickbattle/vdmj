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

package com.fujitsu.vdmj.tc.patterns;

import com.fujitsu.vdmj.tc.patterns.visitors.TCMultipleBindVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCMultipleTypeBind extends TCMultipleBind
{
	private static final long serialVersionUID = 1L;
	public TCType type;
	public final TCTypeList unresolved;

	public TCMultipleTypeBind(TCPatternList plist, TCType type)
	{
		super(plist);
		this.type = type;
		this.unresolved = type.unresolvedTypes();
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
		type = type.typeResolve(base);
		TCType ptype = getPossibleType();
		
		TypeComparator.checkImports(base, unresolved, location.module);
		TypeComparator.checkComposeTypes(type, base, false);

		if (!TypeComparator.compatible(ptype, type))
		{
			type.report(3265, "At least one bind cannot match this type");
			type.detail2("Binds", ptype, "Type", type);
		}

		return type;
	}

	@Override
	public <R, S> R apply(TCMultipleBindVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMultipleTypeBind(this, arg);
	}
}
