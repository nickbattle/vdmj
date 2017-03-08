/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.debug;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Tracepoint;

/**
 * Implement a trace callback for the command line.
 */
public class TraceReader implements TraceCallback
{
	@Override
	public void tracepoint(Context ctxt, Tracepoint tp)
	{
		if (tp.condition == null)
		{
			String s = "Reached trace point [" + tp.number + "]";
			Console.out.println(Thread.currentThread().getName() + ": " + s);
		}
		else
		{
			String result = null;
			
			try
			{
				result = tp.condition.eval(ctxt).toString();
			}
			catch (Exception e)
			{
				result = e.getMessage();
			}
			
			String s = tp.trace + " = " + result + " at trace point [" + tp.number + "]";
			Console.out.println(Thread.currentThread().getName() + ": " + s);
		}
	}
}
