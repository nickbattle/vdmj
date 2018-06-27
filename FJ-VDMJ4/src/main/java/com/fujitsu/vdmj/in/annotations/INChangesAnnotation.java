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

package com.fujitsu.vdmj.in.annotations;

import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INStringLiteralExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
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
	
	protected static void doInit()
	{
		previousState.clear();
		previousLocs.clear();
	}
	
	@Override
	public void after(Context caller, Value rv, INStatement stmt)
	{
		if (Settings.annotations)
		{
			if (!(rv instanceof VoidValue))
			{
				header();
				Console.err.println("RESULT = " + rv);
			}
			
			recordChanges(caller.getVisibleVariables(), caller);
			Context global = caller.getGlobal();
			recordChanges(global, caller);

			if (global instanceof StateContext)
			{
				Context state = ((StateContext)global).stateCtxt;
				
				if (state != null)
				{
					recordChanges(state, caller);
				}
			}
		}
	}
	
	private void recordChanges(Context visible, Context caller)
	{
		for (TCNameToken var: visible.keySet())
		{
			Value curr = visible.get(var);
			
			if (!(curr instanceof FunctionValue) && !(curr instanceof OperationValue))
			{
				String currloc = getLoc(caller, var);
				
				Value prev = previousState.get(var);
				String prevloc = previousLocs.get(var);
				
				if (prevloc == null || !prevloc.equals(currloc))	// New name or different name
				{
					header();
					Console.err.printf("New %s = %s\n", var, curr);
					previousState.put(var, curr.getConstant());
					previousLocs.put(var, currloc);
				}
				else if (prevloc.equals(currloc) && !prev.equals(curr))
				{
					header();
					Console.err.printf("Change %s = %s\n", var, curr);
					previousState.put(var, curr.getConstant());
					previousLocs.put(var, currloc);
				}
				// else it hasn't changed
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
			
			frame = frame.outer;
		}
		
		return "?";
	}

	private void header()
	{
		Console.err.print(name.getLocation() + " ");
		
		if (!args.isEmpty() && args.get(0) instanceof INStringLiteralExpression)
		{
			INStringLiteralExpression s = (INStringLiteralExpression)args.get(0);
			Console.err.print(s.value.value + " ");
		}
	}
}
