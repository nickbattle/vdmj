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

package com.fujitsu.vdmj.in.patterns;

import com.fujitsu.vdmj.in.types.GetAllValues;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.ValueList;

public class INTypeBind extends INBind
{
	private static final long serialVersionUID = 1L;
	public final TCType type;

	public INTypeBind(INPattern pattern, TCType type)
	{
		super(pattern.location, pattern);
		this.type = type;
	}

	@Override
	public INMultipleBindList getMultipleBindList()
	{
		INPatternList plist = new INPatternList();
		plist.add(pattern);
		INMultipleBindList mblist = new INMultipleBindList();
		mblist.add(new INMultipleTypeBind(plist, type));
		return mblist;
	}

	@Override
	public String toString()
	{
		return pattern + ":" + type;
	}

	@Override
	public ValueList getBindValues(Context ctxt, boolean permuted) throws ValueException
	{
		return GetAllValues.ofType(type, ctxt);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		return new ValueList();
	}

	@Override
	public TCNameList getOldNames()
	{
		return new TCNameList();
	}
}
