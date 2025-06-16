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

package com.fujitsu.vdmj.runtime;

import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;

/**
 * A catchpoint when an exception is raised.
 */
public class Catchpoint extends Breakpoint
{
	private static final long serialVersionUID = 1L;
	private final String value;

	public Catchpoint(String value, int number) throws Exception
	{
		super(LexLocation.ANY, number, null);
		this.value = value;
	}

	/**
	 * This extra constructor adds a specific location, when the catchpoint is
	 * triggered, indicating the location of the "exit". 
	 */
	public Catchpoint(LexLocation throwloc, String value, int number) throws Exception
	{
		super(throwloc, number, null);
		this.value = value;
	}

	public boolean check(LexLocation exitloc, Context ctxt, Value thrown)
	{
		boolean matched = false;
		
		if (thrown instanceof ObjectValue)
		{
			ObjectValue obj = (ObjectValue)thrown;
			matched = hasSupertype(obj.type, value);	// Just use the class name
		}
		else
		{
			matched = thrown.toString().equals(value);
		}
		
		if (value == null || matched)
		{
			try
			{
				hits++;
				DebugLink.getInstance().breakpoint(ctxt, new Catchpoint(exitloc, value, number));
				return true;
			}
			catch (Exception e)
			{
				println("Catchpoint [" + number + "]: " + e.getMessage());
			}
		}
		
		return false;
	}

	@Override
	public String toString()
	{
		return "catch [" + number + "] " + (value == null ? "(all exceptions)" : "value = " + value);
	}
	
	private boolean hasSupertype(TCClassType type, String name)
	{
		if (type.name.getName().equals(name))
		{
			return true;
		}
		else
		{
			for (TCType stype: type.classdef.supertypes)
			{
				TCClassType sclass = (TCClassType)stype;

				if (hasSupertype(sclass, name))
				{
					return true;
				}
			}
		}

		return false;
	}
}
