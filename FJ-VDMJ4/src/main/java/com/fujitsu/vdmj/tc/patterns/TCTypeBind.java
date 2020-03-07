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

import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCTypeBind extends TCBind
{
	private static final long serialVersionUID = 1L;
	public TCType type;

	public TCTypeBind(TCPattern pattern, TCType type)
	{
		super(pattern.location, pattern);
		this.type = type;
	}

	public void typeResolve(Environment env)
	{
		type = type.typeResolve(env, null);
	}

	@Override
	public TCMultipleBindList getMultipleBindList()
	{
		TCPatternList plist = new TCPatternList();
		plist.add(pattern);
		TCMultipleBindList mblist = new TCMultipleBindList();
		mblist.add(new TCMultipleTypeBind(plist, type));
		return mblist;
	}

	@Override
	public String toString()
	{
		return pattern + ":" + type;
	}

	@Override
	public TCTypeSet exitCheck(Environment base)
	{
		return new TCTypeSet();
	}

	@Override
	public <R, S> R apply(TCBindVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTypeBind(this, arg);
	}
}
