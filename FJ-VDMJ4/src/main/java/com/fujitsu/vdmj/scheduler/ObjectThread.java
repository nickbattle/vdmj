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
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.TransactionValue;
import com.fujitsu.vdmj.values.ValueList;

/**
 * A class representing a VDM thread running in an object.
 */
public class ObjectThread extends SchedulableThread
{
	private static final long serialVersionUID = 1L;
	public final OperationValue operation;
	public final Context ctxt;
	public final String title;
	public final boolean breakAtStart;

	public ObjectThread(LexLocation location, ObjectValue object, Context ctxt)
		throws ValueException
	{
		super(object.getCPU().resource, object, 0, false, 0);

		setName("ObjectThread-" + getId());

		this.title =
			"Thread " + getId() +
			", self #" + object.objectReference +
			", class " + object.type.name.getName();

		this.ctxt = new ObjectContext(location, title, ctxt.getGlobal(), object);
		this.operation = object.getThreadOperation(ctxt);
		this.breakAtStart = ctxt.threadState.isStepping();
	}

	@Override
	public int hashCode()
	{
		return (int)getId();
	}

	@Override
	public void body()
	{
//		if (Settings.usingDBGP)
//		{
//			runDBGP();
//		}
//		else
		{
			runCmd();
		}
	}

	private void runCmd()
	{
		try
		{
			ctxt.setThreadState(operation.getCPU());

			if (breakAtStart)
			{
				// Step at the first location you check (start of body)
				ctxt.threadState.setBreaks(new LexLocation(), null, null);
			}

			operation.eval(ctxt.location, new ValueList(), ctxt);
		}
		catch (ValueException e)
		{
			suspendOthers();
			ResourceScheduler.setException(e);
			DebugLink.getInstance().stopped(e.ctxt, e.ctxt.location);
		}
		catch (ContextException e)
		{
			suspendOthers();
			ResourceScheduler.setException(e);
			DebugLink.getInstance().stopped(e.ctxt, e.location);
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			ResourceScheduler.setException(e);
			SchedulableThread.signalAll(Signal.SUSPEND);
		}
		finally
		{
			TransactionValue.commitAll();
		}
	}
}
