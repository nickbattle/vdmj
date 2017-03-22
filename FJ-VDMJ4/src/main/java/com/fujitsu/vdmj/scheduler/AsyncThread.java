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

import com.fujitsu.vdmj.dbgp.DBGPReason;
import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.TransactionValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class AsyncThread extends SchedulableThread
{
	private static final long serialVersionUID = 1L;
	public final MessageRequest request;

	public final ObjectValue self;
	public final OperationValue operation;
	public final ValueList args;
	public final CPUValue cpu;
	public final boolean breakAtStart;

	public AsyncThread(MessageRequest request)
	{
		super(
			request.target.getCPU().resource,
			request.target,
			request.operation.getPriority(),
			false, 0);

		setName("Async-" + object.type.name.getName() + "-" + getId());

		this.self = request.target;
		this.operation = request.operation;
		this.args = request.args;
		this.cpu = self.getCPU();
		this.breakAtStart = request.breakAtStart;
		this.request = request;
	}

	@Override
	protected void body()
	{
		RootContext global = ClassInterpreter.getInstance().getInitialContext();
		LexLocation from = self.type.classdef.location;
		Context ctxt = new ObjectContext(from, "async", global, self);
		DebugLink link = DebugLink.getInstance();

		try
		{
			link.setCPU(operation.getCPU());
    		ctxt.setThreadState(cpu);

			if (breakAtStart)
			{
				// Step at the first location you check (start of body)
				ctxt.threadState.setBreaks(new LexLocation(), null, null);
			}

    		Value result = operation.localEval(
    			operation.name.getLocation(), args, ctxt, false);

			if (request.replyTo != null)
			{
				request.bus.reply(new MessageResponse(result, request));
			}

			link.complete(DBGPReason.OK, null);
		}
		catch (ValueException e)
		{
			suspendOthers();
			ResourceScheduler.setException(e);
			link.stopped(e.ctxt, e.ctxt.location);
		}
		catch (ContextException e)
		{
			suspendOthers();
			ResourceScheduler.setException(e);
			link.stopped(e.ctxt, e.location);
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
