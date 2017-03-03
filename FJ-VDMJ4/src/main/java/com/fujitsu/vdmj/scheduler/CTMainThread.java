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

package com.fujitsu.vdmj.scheduler;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.traces.INTraceVariableStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.traces.CallSequence;
import com.fujitsu.vdmj.traces.Verdict;

/**
 * A class representing the main VDM thread.
 */
public class CTMainThread extends MainThread
{
	private static final long serialVersionUID = 1L;
	private final CallSequence test;
	private final boolean debug;

	private List<Object> result = new Vector<Object>();

	public CTMainThread(CallSequence test, Context ctxt, boolean debug)
	{
		super(null, ctxt);

		this.test = test;
		this.debug = debug;

		setName("CTMainThread");
	}

	@Override
	public int hashCode()
	{
		return (int)getId();
	}

	@Override
	public void body()
	{
		try
		{
			for (INStatement statement: test)
			{
				if (statement instanceof INTraceVariableStatement)
				{
					// Just update the context...
					statement.eval(ctxt);
				}
				else
				{
 					result.add(statement.eval(ctxt));
				}
			}

			result.add(Verdict.PASSED);
		}
		catch (ContextException e)
		{
			result.add(e.getMessage());

			if (debug)
			{
				setException(e);
				suspendOthers();

				DebugLink.getInstance().stopped(e.ctxt, e.location);
				result.add(Verdict.FAILED);
			}
			else
			{
				// These exceptions are inconclusive if they occur
				// in a call directly from the test because it could
				// be a test error, but if the test call has made
				// further call(s), then they are real failures.

    			switch (e.number)
    			{
    				case 4055:	// precondition fails for functions

    					if (e.ctxt.outer != null && e.ctxt.outer.outer == ctxt)
    					{
    						result.add(Verdict.INCONCLUSIVE);
    					}
    					else
    					{
    						result.add(Verdict.FAILED);
    					}
    					break;

    				case 4071:	// precondition fails for operations

    					if (e.ctxt.outer == ctxt)
    					{
    						result.add(Verdict.INCONCLUSIVE);
    					}
    					else
    					{
    						result.add(Verdict.FAILED);
    					}
    					break;

    				default:
    					if (e.ctxt == ctxt)
    					{
    						result.add(Verdict.INCONCLUSIVE);
    					}
    					else
    					{
    						result.add(Verdict.FAILED);
    					}
    					break;
    			}
			}
		}
		catch (Throwable e)
		{
			if (result.lastIndexOf(Verdict.FAILED) < 0)
			{
				if (getException() != null)
				{
					result.add(getException());
				}
				else
				{
					result.add(e.getMessage());
				}
				
				result.add(Verdict.FAILED);
			}
		}
	}

	@Override
	protected void handleSignal(Signal sig, Context lctxt, LexLocation location)
	{
		if (sig == Signal.DEADLOCKED)
		{
			result.add("DEADLOCK detected");
			result.add(Verdict.FAILED);
		}

		super.handleSignal(sig, lctxt, location);
	}

	@Override
	public void setException(Exception e)
	{
		// Don't print out the error for CT
		exception = e;
	}

	public List<Object> getList()
	{
		return result;
	}
}
