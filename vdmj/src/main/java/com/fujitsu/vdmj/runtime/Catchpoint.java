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

package com.fujitsu.vdmj.runtime;

import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.lex.LexLocation;
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
		
		if (thrown instanceof ObjectValue)		// Just use the class name
		{
			ObjectValue obj = (ObjectValue)thrown;
			matched = obj.type.name.getName().equals(value);
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
}
