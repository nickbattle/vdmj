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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.runtime;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.ConsoleWriter;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.Value;

/**
 * A root context for non-object method invocations.
 */
public class StateContext extends RootContext
{
	/** The state context, if any. */
	public final Context stateCtxt;

	/**
	 * Create a RootContext from the values passed.
	 *
	 * @param location The location of the context.
	 * @param title The name of the location.
	 * @param outer The context chain (not searched).
	 * @param sctxt Any state context.
	 */

	public StateContext(LexLocation location, String title,
		Context freeVariables, Context outer, Context sctxt)
	{
		super(location, title, freeVariables, outer);
		this.stateCtxt = sctxt;
	}

	public StateContext(LexLocation location, String title,
		Context outer, Context sctxt)
	{
		this(location, title, null, outer, sctxt);
	}

	/**
	 * Create a RootContext with no outer context or state.
	 * @param location The location of the context.
	 * @param title The name of the location.
	 */

	public StateContext(LexLocation location, String title)
	{
		super(location, title, null, null);
		this.stateCtxt = null;
	}

	/**
	 * Check for the name in the current context and state, and if
	 * not present search the global context. Note that the context
	 * chain is not followed.
	 */
	@Override
	public Value check(TCNameToken name)
	{
		Value v = get(name);

		if (v != null)
		{
			return v;
		}

		if (freeVariables != null)
		{
			v = freeVariables.get(name);

			if (v != null)
			{
				return v;
			}
		}

		// A RootContext stops the name search from continuing down the
		// context chain. It first checks any state context, then goes
		// down to the global level.

		if (v == null)
		{
			if (stateCtxt != null)
			{
				v = stateCtxt.check(name);

				if (v != null)
				{
					return v;
				}
			}

			Context g = getGlobal();

			if (g != this)
			{
				return g.check(name);
			}
		}

		return v;
	}

	@Override
	public TCNameList getVisibleNames()
	{
		TCNameList names = new TCNameList();

		Context g = getGlobal();

		if (g != this)
		{
			names.addAll(g.getVisibleNames());
		}
		
		if (freeVariables != null)
		{
			names.addAll(freeVariables.keySet());
		}
		
		if (stateCtxt != null)
		{
			names.addAll(stateCtxt.keySet());
		}
		
		names.addAll(keySet());
		return names;
	}

	@Override
	public String toString()
	{
		if (stateCtxt != null)
		{
			return super.toString() + "\tState visible\n";
		}
		else
		{
			return super.toString();
		}
	}

	@Override
	public void printStackTrace(ConsoleWriter out, boolean variables)
	{
		if (outer == null)		// Don't expand initial context
		{
			out.println("In root context of " + title);
		}
		else
		{
			if (variables)
			{
    			out.print(this.format("\t", this));

				if (freeVariables != null && !freeVariables.isEmpty())
				{
					out.println("Free Variables:");
					out.print(this.format("\t", freeVariables));
				}

    			if (stateCtxt != null)
    			{
    				out.println("\tState visible");
    			}
			}

			out.println("In root context of " + title + " " + location);
			outer.printStackTrace(out, false);
		}
	}
}
