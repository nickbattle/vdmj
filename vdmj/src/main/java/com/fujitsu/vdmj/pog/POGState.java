/*******************************************************************************
 *
 *	Copyright (c) 2024 Nick Battle.
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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A class to hold state information for POG of statements, which involve potentially
 * changing state variables.
 */
public class POGState
{
	private static final TCNameToken SOMETHING = new TCNameToken(LexLocation.ANY, "?", "?");
	
	private final Map<TCNameToken, LexLocation> updatedState;
	private final Map<TCNameToken, LexLocation> updatedLocals;
	private final POGState outerState;
	private final TCNameList localNames;
	
	private boolean ambiguousExpression = false;
	
	public POGState()
	{
		this.updatedState = new HashMap<TCNameToken, LexLocation>();
		this.updatedLocals = new HashMap<TCNameToken, LexLocation>();
		this.outerState = null;
		this.localNames = new TCNameList();
	}
	
	/**
	 * Used by getCopy and getLink.
	 */
	private POGState(Map<TCNameToken, LexLocation> updatedState, Map<TCNameToken, LexLocation> updatedLocals,
			POGState outerState, TCNameList localNames)
	{
		this.updatedState = updatedState;
		this.updatedLocals = updatedLocals;
		this.outerState = outerState;
		this.localNames = localNames;
	}
	
	@Override
	public String toString()
	{
		return "state: " + updatedState.keySet() +
				", locals: " + updatedLocals.keySet() +
				(outerState != null ? " / " + outerState.toString() : "");
	}
	
	/**
	 * Copy a state for use in if/else branches etc, where changes in each are not visible
	 * in the other branches, but all changes are combined afterwards. Note that it has
	 * the same local names and ambiguous state, but no outer state changes.
	 */
	public POGState getCopy()
	{
		return new POGState(
				new HashMap<TCNameToken, LexLocation>(),
				new HashMap<TCNameToken, LexLocation>(),
				null, localNames);
	}
	
	/**
	 * Create a new chained POGState, linked to the current one. This is used to process
	 * block statements that may contain "dcl" statements (ie. local state). Locals are
	 * added with addDclLocal.
	 */
	public POGState getLink()
	{
		return new POGState(
				new HashMap<TCNameToken, LexLocation>(),
				new HashMap<TCNameToken, LexLocation>(),
				this, new TCNameList());
	}
	
	/**
	 * True if state has been updated, either here or in outer levels.
	 */
	public boolean hasUpdatedState(Collection<TCNameToken> names)
	{
		if (updatedState.containsKey(SOMETHING))
		{
			return true;
		}

		for (TCNameToken name: names)
		{
			if (localNames.contains(name))
			{
				return updatedLocals.containsKey(name);
			}
			else if (updatedState.containsKey(name))
			{
				return true;
			}
		}
		
		return (outerState != null && outerState.hasUpdatedState(names));
	}
	
	/**
	 * True if POGState has a local variable of this name.
	 */
	public boolean hasLocalName(TCNameToken name)
	{
		return localNames.contains(name);
	}

	/**
	 * Return the location of the last state update.
	 */
	public LexLocation getUpdatedLocation(Collection<TCNameToken> names)
	{
		for (TCNameToken name: names)
		{
			if (localNames.contains(name))
			{
				if (updatedLocals.containsKey(name))
				{
					return updatedLocals.get(name);
				}
			}
			else
			{
				if (updatedState.containsKey(name))
				{
					return updatedState.get(name);
				}
				
				if (updatedState.containsKey(SOMETHING))
				{
					return updatedState.get(SOMETHING);
				}
			}
		}

		if (outerState != null)
		{
			return outerState.getUpdatedLocation(names);
		}
		
		return LexLocation.ANY;
	}
	
	/**
	 * State updates either update the updatedLocals of the nearest getLink, or they
	 * update the updatedState in the lowest level. 
	 */
	
	public void didUpdateState(LexLocation from)
	{
		if (outerState != null)
		{
			outerState.didUpdateState(from);	// Find outermost level
		}
		else
		{
			updatedState.put(SOMETHING, from);
		}
	}

	public void didUpdateState(TCNameToken name, LexLocation from)
	{
		if (localNames.contains(name))
		{
			updatedLocals.put(name, from);			// A local dcl update
		}
		else if (outerState != null)
		{
			outerState.didUpdateState(name, from);	// May be an outer* local, or state
		}
		else
		{
			updatedState.put(name, from);			// An outermost state update
		}
	}

	public void didUpdateState(Collection<TCNameToken> names, LexLocation from)
	{
		for (TCNameToken name: names)
		{
			didUpdateState(name, from);
		}
	}
	
	public void addDclLocal(TCNameToken name)
	{
		localNames.add(name);
	}
	
	public void addDclLocal(Collection<TCNameToken> names)
	{
		for (TCNameToken name: names)
		{
			localNames.add(name);
		}
	}

	/**
	 * Combine copies for if/else branches, created by getCopy().
	 */
	public void combineWith(POGState copy)
	{
		updatedState.putAll(copy.updatedState);
		updatedLocals.putAll(copy.updatedLocals);
	}
	
	/**
	 * Mark an expression as ambiguous - used in let def to mark locals as ambiguous.
	 */
	public void setAmbiguous(boolean ambiguous)
	{
		ambiguousExpression = ambiguous;
	}
	
	public boolean isAmbiguous()
	{
		return ambiguousExpression;
	}
}
