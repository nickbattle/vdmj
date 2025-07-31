/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package com.fujitsu.vdmj.pog;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.tc.lex.TCNameSet;

/**
 * A POContext that represents a set of alternative paths in an operation PO.
 */
public class POAltContext extends POContext
{
	public final List<POContextStack> alternatives;
	
	public POAltContext()
	{
		alternatives = new Vector<POContextStack>();
	}
	
	public POAltContext(List<POContextStack> paths)
	{
		this();
		alternatives.addAll(paths);
	}

	/**
	 * Add a particular context stack to the list of alternatives.
	 */
	public POContextStack add(POContextStack stack)
	{
		alternatives.add(stack);
		return stack;
	}

	/**
	 * Add a list of specific context stacks to the list of alternatives.
	 */
	public void addAll(List<POContextStack> paths)
	{
		for (POContextStack path: paths)
		{
			add(path);
		}
	}
	
	/**
	 * Create an empty context stack and add it to the alternatives, then return
	 * the new stack to be filled (presumably).
	 */
	public POContextStack add()
	{
		return add(new POContextStack());
	}

	public boolean isEmpty()
	{
		return alternatives.isEmpty();
	}

	@Override
	public TCNameSet ambiguousVariables()
	{
		TCNameSet set = new TCNameSet();
		
		for (POContextStack stack: alternatives)
		{
			for (POContext ctxt: stack)
			{
				set.addAll(ctxt.ambiguousVariables());
			}
		}
		
		return set;
	}

	@Override
	public TCNameSet resolvedVariables()
	{
		TCNameSet set = new TCNameSet();
		
		for (POContextStack stack: alternatives)
		{
			for (POContext ctxt: stack)
			{
				set.addAll(ctxt.resolvedVariables());
			}
		}
		
		return set;
	}

	@Override
	public TCNameSet reasonsAbout()
	{
		if (alternatives.isEmpty())
		{
			return null;
		}
		else
		{
			TCNameSet set = new TCNameSet();
			
			for (POContextStack stack: alternatives)
			{
				set.addAll(stack.getReasonsAbout());
			}
			
			return set;
		}
	}

	@Override
	public String getSource()
	{
		return "";
	}
}
