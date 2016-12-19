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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.scheduler.ObjectThread;
import com.fujitsu.vdmj.scheduler.PeriodicThread;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueSet;
import com.fujitsu.vdmj.values.VoidValue;

public class INStartStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INExpression objects;

	public INStartStatement(LexLocation location, INExpression obj)
	{
		super(location);
		this.objects = obj;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
			Value value = objects.eval(ctxt);

			if (value.isType(SetValue.class))
			{
				ValueSet set = value.setValue(ctxt);

				for (Value v: set)
				{
					ObjectValue target = v.objectValue(ctxt);
					OperationValue op = target.getThreadOperation(ctxt);

					start(target, op, ctxt);
				}
			}
			else
			{
				ObjectValue target = value.objectValue(ctxt);
				OperationValue op = target.getThreadOperation(ctxt);

				start(target, op, ctxt);
			}

			return new VoidValue();
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	private void start(ObjectValue target, OperationValue op, Context ctxt)
		throws ValueException
	{
		if (op.body instanceof INPeriodicStatement)
		{
    		RootContext global = ClassInterpreter.getInstance().getInitialContext();
    		Context pctxt = new ObjectContext(op.name.getLocation(), "periodic", global, target);
			INPeriodicStatement ps = (INPeriodicStatement)op.body;
			
			// We disable the swapping and time (RT) as periodic evaluation should be "free".
			try
			{
				pctxt.threadState.setAtomic(true);
				pctxt.threadState.setPure(true);
				ps.eval(pctxt);	// Ignore return value
			}
			finally
			{
				pctxt.threadState.setAtomic(false);
				pctxt.threadState.setPure(false);
			}
			
			OperationValue pop = pctxt.lookup(ps.opname).operationValue(pctxt);

			long period = ps.values[0];
			long jitter = ps.values[1];
			long delay  = ps.values[2];
			long offset = ps.values[3];

			// Note that periodic threads never set the stepping flag

			new PeriodicThread(
				target, pop, period, jitter, delay, offset, 0, false).start();
		}
		else if (op.body instanceof INSporadicStatement)
		{
    		RootContext global = ClassInterpreter.getInstance().getInitialContext();
    		Context pctxt = new ObjectContext(op.name.getLocation(), "sporadic", global, target);
    		INSporadicStatement ss = (INSporadicStatement)op.body;
			
			// We disable the swapping and time (RT) as sporadic evaluation should be "free".
    		try
    		{
    			pctxt.threadState.setAtomic(true);
				pctxt.threadState.setPure(true);
    			ss.eval(pctxt);	// Ignore return value
    		}
    		finally
    		{
    			pctxt.threadState.setAtomic(false);
				pctxt.threadState.setPure(false);
    		}
			
			OperationValue pop = pctxt.lookup(ss.opname).operationValue(pctxt);

			long delay  = ss.values[0];
			long jitter = ss.values[1];		// Jitter used for maximum delay
			long offset = ss.values[2];
			long period = 0;

			// Note that periodic threads never set the stepping flag

			new PeriodicThread(
				target, pop, period, jitter, delay, offset, 0, true).start();
		}
		else
		{
			new ObjectThread(location, target, ctxt).start();
		}
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		return objects.findExpression(lineno);
	}

	@Override
	public String toString()
	{
		return "start(" + objects + ")";
	}
}
