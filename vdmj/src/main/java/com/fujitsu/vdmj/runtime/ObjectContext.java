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
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;

/**
 * A root context for object method invocations.
 */
public class ObjectContext extends RootContext
{
	public final ObjectValue self;

	/**
	 * Create an ObjectContext from the values passed.
	 *
	 * @param location The location of the context.
	 * @param title The name of the location.
	 * @param outer The context chain (not searched).
	 * @param self The object context.
	 */
	public ObjectContext(
		LexLocation location, String title, Context freeVariables,
		Context outer, ObjectValue self)
	{
		super(location, title, freeVariables, outer);
		this.self = self;
	}

	public ObjectContext(
		LexLocation location, String title, Context outer, ObjectValue self)
	{
		this(location, title, null, outer, self);
	}

	/**
	 * Check for the name in the current context and self, and if
	 * not present search the global context. Note that the context
	 * chain is not followed.
	 */
	@Override
	public Value check(TCNameToken name)
	{
		// A RootContext stops the name search from continuing down the
		// context chain. It first checks any local context, then it
		// checks the "self" context, then it goes down to the global level.

		Value v = get(name);		// Local variables

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

		v = self.get(name, name.isExplicit());

		if (v != null)
		{
			return v;
		}

		Context g = getGlobal();

		if (g != this)
		{
			return g.check(name);
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
		
		names.addAll(self.members.keySet());
		names.addAll(keySet());
		return names;
	}

	@Override
	public String toString()
	{
		return super.toString();	// Self there anyway ...+ self.toString();
	}

	@Override
	public void printStackTrace(ConsoleWriter out, boolean variables)
	{
		if (outer == null)		// Don't expand initial context
		{
			out.println("In object context of " + title);
		}
		else
		{
			if (variables)
			{
    			out.print(this.format("\t", this));
			}

			out.println("In object context of " + title + " " + location);
			outer.printStackTrace(out, false);
		}
	}

	@Override
	public ObjectValue getSelf()
	{
		return self;
	}
}
