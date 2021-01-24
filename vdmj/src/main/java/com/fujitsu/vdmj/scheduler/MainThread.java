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

import java.lang.reflect.InvocationTargetException;

import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.debug.DebugReason;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.TransactionValue;
import com.fujitsu.vdmj.values.UndefinedValue;
import com.fujitsu.vdmj.values.Value;

/**
 * A class representing the main VDM thread.
 */
public class MainThread extends SchedulableThread
{
	private static final long serialVersionUID = 1L;
	public final Context ctxt;
	public final INExpression expression;

	private Value result = new UndefinedValue();
	protected Exception exception = null;

	public MainThread(INExpression expr, Context ctxt)
	{
		super(CPUResource.vCPU, null, 0, false, 0);

		this.expression = expr;
		this.ctxt = ctxt;
		this.exception = null;

		setName("MainThread");
	}

	@Override
	public int hashCode()
	{
		return (int)getId();
	}

	@Override
	public void body()
	{
		DebugLink link = DebugLink.getInstance();
		DebugReason completeReason = DebugReason.OK;
		Breakpoint.setExecInterrupt(Breakpoint.NONE);

		try
		{
			link.newThread(CPUValue.vCPU);
			result = expression.eval(ctxt);
		}
		catch (ContextException e)
		{
			setException(e);
			suspendOthers();
			link.stopped(e.ctxt, e.location, e);
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			setException(e);
			suspendOthers();
		}
		catch (ThreadDeath e)
		{
			completeReason = DebugReason.ABORTED;
			throw e;
		}
		catch (Throwable th)	// Java errors not caught above
		{
			setException(new Exception(th.getMessage()));
			suspendOthers();
		}
		finally
		{
			TransactionValue.commitAll();
			link.complete(completeReason, null);
		}
	}

	public Value getResult() throws Exception
	{
		if (exception != null)
		{
			throw exception;
		}

		return result;
	}

	public void setException(Exception e)
	{
		exception = e;
	}
	
	public Exception getException()
	{
		return exception;
	}
}
