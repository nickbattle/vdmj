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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueListenerList;
import com.fujitsu.vdmj.values.VoidValue;

public class INAtomicStatement extends INStatement
{
	private static final long serialVersionUID = 1L;

	public final INAssignmentStatementList assignments;

	public INAtomicStatement(LexLocation location, INAssignmentStatementList assignments)
	{
		super(location);
		this.assignments = assignments;
	}

	@Override
	public String toString()
	{
		return "atomic (" + Utils.listToString(assignments) + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		
		int size = assignments.size();
		ValueList targets = new ValueList(size);
		ValueList values = new ValueList(size);
		
		// Rather than execute the assignment statements directly, we calculate the
		// Updateable values that would be affected, and the new values to put in them.
		// Note that this does not provoke any invariant checks (other than those that
		// may be present in the RHS expression of each assignment).
		
		for (INAssignmentStatement stmt: assignments)
		{
			try
			{
				stmt.location.hit();
				targets.add(stmt.target.eval(ctxt));
				values.add(stmt.exp.eval(ctxt).convertTo(stmt.targetType, ctxt));
			}
			catch (ValueException e)
			{
				abort(e);
			}
		}
		
		// We make the assignments atomically by turning off thread swaps and time
		// then temporarily removing the listener lists from each Updateable target.
		// Then, when all assignments have been made, we check the invariants by
		// passing the updated values to each listener list, as the assignment would have.
		// Finally, we re-enable the thread swapping and time stepping, before returning
		// a void value.
		
		try
		{
			ctxt.threadState.setAtomic(true);
			List<ValueListenerList> listenerLists = new Vector<ValueListenerList>(size);
	
			for (int i = 0; i < size; i++)
			{
				UpdatableValue target = (UpdatableValue) targets.get(i);
				listenerLists.add(target.listeners);
				target.listeners = null;
				target.set(location, values.get(i), ctxt);	// No invariant listeners
				target.listeners = listenerLists.get(i);
			}
			
			for (int i = 0; i < size; i++)
			{
				ValueListenerList listeners = listenerLists.get(i);
				
				if (listeners != null)
				{
					listeners.changedValue(location, values.get(i), ctxt);
				}
			}
		}
		catch (ValueException e)
		{
			abort(e);
		}
		finally
		{
			ctxt.threadState.setAtomic(false);
		}
		
		return new VoidValue();
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAtomicStatement(this, arg);
	}
}
