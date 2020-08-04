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

import com.fujitsu.vdmj.in.patterns.visitors.INMultipleBindVisitor;
import com.fujitsu.vdmj.in.types.GetAllValues;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.ValueList;

public class INMultipleTypeBind extends INMultipleBind
{
	private static final long serialVersionUID = 1L;
	public final TCType type;

	public INMultipleTypeBind(INPatternList plist, TCType type)
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
	public ValueList getBindValues(Context ctxt, boolean permuted) throws ValueException
	{
		return GetAllValues.ofType(type, ctxt);
	}

	@Override
	public <R, S> R apply(INMultipleBindVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMultipleTypeBind(this, arg);
	}
}
