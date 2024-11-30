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

import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.statements.POExternalClause;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A class to hold state information for POG of statements, which involve potentially
 * changing state variables.
 */
public class POGState
{
	private static final TCNameToken SOMETHING = new TCNameToken(LexLocation.ANY, "?", "?");
	
	private Map<TCNameToken, LexLocation> updatedState;
	private Map<TCNameToken, LexLocation> updatedLocals;
	private POGState outerState;
	private TCNameList localNames;
	
	public POGState()
	{
		updatedState = new HashMap<TCNameToken, LexLocation>();
		updatedLocals = new HashMap<TCNameToken, LexLocation>();
		outerState = null;
		localNames = new TCNameList();
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
		return "state: " + updatedState.toString() +
				", locals: " + updatedLocals.toString() +
				(outerState != null ? " / " + outerState.toString() : "");
	}
	
	/**
	 * Copy a state for use in if/else branches etc, where changes in each are not visible
	 * in the other branches, but all changes are combined afterwards. Note that it has
	 * the same local names and outer state.
	 */
	public POGState getCopy()
	{
		HashMap<TCNameToken, LexLocation> copyState = new HashMap<TCNameToken, LexLocation>();
		HashMap<TCNameToken, LexLocation> copyLocals = new HashMap<TCNameToken, LexLocation>();
		
		return new POGState(copyState, copyLocals, outerState, localNames);
	}
	
	/**
	 * Create a new chained POGState, linked to the current one. This is used to process
	 * block statements that may contain "dcl" statements (ie. local state). The new local
	 * state initially has no updates.
	 */
	public POGState getLink()
	{
		return new POGState(
			new HashMap<TCNameToken, LexLocation>(),
			new HashMap<TCNameToken, LexLocation>(), this, new TCNameList());
	}
	
	/**
	 * True if state has been updated, either here or in outer levels.
	 */
	public boolean hasUpdatedState(TCNameSet names)
	{
		for (TCNameToken name: names)
		{
			if (localNames.contains(name))
			{
				return updatedLocals.containsKey(name);
			}
			else
			{
				if (updatedState.containsKey(name) ||
					updatedState.containsKey(SOMETHING))
				{
					return true;
				}
			}
		}
		
		return (outerState != null && outerState.hasUpdatedState(names));
	}

	/**
	 * Return the location of the last state update.
	 */
	public LexLocation getUpdatedLocation(TCNameSet names)
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
	
	public void didUpdateState(LexLocation from)
	{
		if (outerState != null)
		{
			outerState.didUpdateState(from);
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
			updatedState.put(name, from);			// A module state update
		}
	}

	public void didUpdateState(TCNameList names, LexLocation from)
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
	
	public void addOperationCall(LexLocation from, PODefinition called)
	{
		if (called == null)
		{
			didUpdateState(from);	// Assumed
		}
		else if (called.accessSpecifier.isPure)
		{
			return;		// No updates, by definition
		}
		else if (called instanceof POImplicitOperationDefinition)
		{
			POImplicitOperationDefinition imp = (POImplicitOperationDefinition)called;
			
			if (imp.externals != null)
			{
				for (POExternalClause ext: imp.externals)
				{
					if (ext.mode.is(Token.WRITE))
					{
						didUpdateState(ext.identifiers, from);
					}
				}
			}
			else
			{
				didUpdateState(from);
			}
		}
		else if (called instanceof POExplicitOperationDefinition)
		{
			didUpdateState(from);
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
}
