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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.values;

import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;

public class ValueListenerList extends Vector<ValueListener>
{
	private static final long serialVersionUID = 1L;

	public ValueListenerList(ValueListener listener)
	{
		add(listener);
	}

	public ValueListenerList(ValueListenerList list)
	{
		addAll(list);
	}
	
	public ValueListenerList()
	{
		// empty
	}

	public void changedValue(LexLocation location, Value value, Context ctxt) throws ValueException
	{
		// Copy the list so that any additions caused by changeValue are not concurrent
		ValueListenerList copy = new ValueListenerList(this);
		
		for (ValueListener vl: copy)
		{
			vl.changedValue(location, value, ctxt);
		}
	}
}
