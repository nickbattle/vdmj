/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package annotations.in;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INStringLiteralExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.ClassContext;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.VoidValue;

public class INChangesAnnotation extends INAnnotation
{
	private final static Context previousState = new Context(new LexLocation(), "@Changes", null);
	private final static Map<TCNameToken, String> previousLocs = new HashMap<TCNameToken, String>();

	public INChangesAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
	}
	
	public static void doInit()
	{
		previousState.clear();
		previousLocs.clear();
	}
	
	@Override
	public void inAfter(INStatement stmt, Value rv, Context caller)
	{
		recordChanges(caller.getVisibleNames(), caller);
		cleanup(caller);

		if (!(rv instanceof VoidValue))
		{
			header();
			System.err.println("RESULT = " + rv);
		}
	}
	
	private void recordChanges(TCNameList visible, Context caller)
	{
		for (TCNameToken var: visible)
		{
			if (var.equals(var.getSelfName()))
			{
				continue;	// Skip selfs
			}
			
			Value curr = caller.check(var);
			
			if (!(curr instanceof FunctionValue) && !(curr instanceof OperationValue))
			{
				String currloc = getLoc(caller, var);
				
				Value prev = previousState.get(var);
				String prevloc = previousLocs.get(var);
				
				if (prevloc == null)	// New name
				{
					header();
					System.err.printf("New %s = %s\n", var, curr);
					previousState.put(var, curr.getConstant());
					previousLocs.put(var, currloc);
				}
				else if (curr.isUndefined() && prev.isUndefined())
				{
					// Unchanged undefined value
				}
				else if (!prevloc.equals(currloc))	// Different name
				{
					header();
					System.err.printf("New %s = %s\n", var, curr);
					previousState.put(var, curr.getConstant());
					previousLocs.put(var, currloc);
				}
				else if (prevloc.equals(currloc) && !prev.toString().equals(curr.toString()))	// for undefineds
				{
					header();
					System.err.printf("Change %s = %s\n", var, curr);
					previousState.put(var, curr.getConstant());
					previousLocs.put(var, currloc);
				}
				// else it hasn't changed
			}
		}
	}

	private void cleanup(Context caller)
	{
		Iterator<TCNameToken> iter = previousState.keySet().iterator();
		
		while (iter.hasNext())
		{
			TCNameToken var = iter.next();
			String currloc = getLoc(caller, var);
			
			if (currloc == null)
			{
				iter.remove();
				previousLocs.remove(var);
			}
		}
	}

	private String getLoc(Context caller, TCNameToken var)
	{
		// Find the declaring frame...
		Context frame = caller;
		
		while (frame != null)
		{
			if (frame.get(var) != null)
			{
				return var.getLocation().toString() + System.identityHashCode(frame);			
			}
			
			if (frame instanceof StateContext && frame.outer != null)
			{
				Context state = ((StateContext)frame).stateCtxt;

				if (state != null && state.get(var) != null)
				{
					return var.getLocation().toString() + System.identityHashCode(state);			
				}
				
				frame = frame.getGlobal();
			}
			else if (frame instanceof ClassContext)
			{
				ClassContext clazz = (ClassContext)frame;

				if (clazz != null && clazz.classdef.getStatic(var) != null)
				{
					return var.getLocation().toString() + System.identityHashCode(clazz.classdef);			
				}
				
				frame = frame.getGlobal();
			}
			else if (frame instanceof ObjectContext)
			{
				ObjectContext object = (ObjectContext)frame;

				if (object != null && object.self.get(var, var.isExplicit()) != null)
				{
					return var.getLocation().toString() + System.identityHashCode(object.self);			
				}
				
				frame = frame.getGlobal();
			}
			else
			{
				frame = frame.outer;
			}
		}
		
		return null;
	}

	private void header()
	{
		System.err.print(name.getLocation() + " ");
		
		if (!args.isEmpty() && args.get(0) instanceof INStringLiteralExpression)
		{
			INStringLiteralExpression s = (INStringLiteralExpression)args.get(0);
			System.err.print(s.value.value + " ");
		}
	}
}
