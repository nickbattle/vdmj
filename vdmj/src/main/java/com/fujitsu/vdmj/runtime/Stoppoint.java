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

package com.fujitsu.vdmj.runtime;

import com.fujitsu.vdmj.lex.LexLocation;

/**
 * A breakpoint where execution must stop.
 */
public class Stoppoint extends Breakpoint
{
	private static final long serialVersionUID = 1L;

	public Stoppoint(LexLocation location, int number, String condition) throws Exception
	{
		super(location, number, condition);
	}

	@Override
	public void check(LexLocation execl, Context ctxt)
	{
		location.hit();
		hits++;

		try
		{
			boolean stop = false;

			try
			{
				ctxt.threadState.setAtomic(true);
				stop = (condition == null) ? true : condition.eval(ctxt).boolValue(ctxt);
			}
			catch (Exception e)
			{
				println("Breakpoint [" + number + "]: " + e.getMessage() + " \"" + trace + "\"");
			}
			finally
			{
				ctxt.threadState.setAtomic(false);
			}

			if (stop)
			{
				enterDebugger(ctxt);
			}
		}
		catch (DebuggerException e)
		{
			throw e;
		}
	}

	@Override
	public String toString()
	{
		if (number == 0)
		{
			return super.toString();
		}
		else
		{
			return "break [" + number + "] " +
				(condition == null ? "" : "when \"" + condition + "\" ") +
				super.toString();
		}
	}
}
