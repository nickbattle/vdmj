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
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold state information for POG of statements, which involve potentially
 * changing state variables.
 */
public class POGState
{
	private Map<TCNameToken, LexLocation> updatedVariables;
	private boolean updatedSomething;
	private LexLocation updatedFrom;
	private POGState outerState;
	private TCNameList locals;
	
	public POGState()
	{
		updatedVariables = new HashMap<TCNameToken, LexLocation>();
		updatedSomething = false;
		updatedFrom = LexLocation.ANY;
		outerState = null;
		locals = new TCNameList();
	}
	
	/**
	 * Used by getCopy and getLink.
	 */
	private POGState(Map<TCNameToken, LexLocation> updatedVariables, boolean updatedSomething,
			LexLocation updatedFrom, POGState outerState, TCNameList locals)
	{
		this.updatedVariables = updatedVariables;
		this.updatedSomething = updatedSomething;
		this.updatedFrom = updatedFrom;
		this.outerState = outerState;
		this.locals = new TCNameList();
		
		this.locals.addAll(locals);
	}
	
	@Override
	public String toString()
	{
		return (updatedSomething ? "changed " + updatedVariables : "no changes") +
				(locals.isEmpty() ? "" : Utils.listToString(" (locals ", locals, ", ", ")"));
	}
	
	/**
	 * Create a copy of the current state, to go into (say) the then/elseif/else branches
	 * of a statement, before combining the results. Note this copies the locals.
	 */
	public POGState getCopy()
	{
		return new POGState(updatedVariables, updatedSomething, updatedFrom, outerState, locals);
	}
	
	/**
	 * Create a new chained POGState, linked to the current one. This is used to process
	 * block statements that may contain "dcl" statements (ie. local state). The new local
	 * state initially has no updates.
	 */
	public POGState getLink()
	{
		return new POGState(new HashMap<TCNameToken, LexLocation>(), false, LexLocation.ANY, this, new TCNameList());
	}
	
	/**
	 * True if state has been updated, either here or in outer levels.
	 */
	public boolean hasUpdatedState()
	{
		return updatedSomething || (outerState != null && outerState.hasUpdatedState());
	}

	public boolean hasUpdatedState(TCNameSet names)
	{
		for (TCNameToken name: names)
		{
			if (updatedVariables.containsKey(name))
			{
				return true;
			}
		}
		
		return (outerState != null && outerState.hasUpdatedState());
	}

	/**
	 * Return the location of the last state update.
	 */
	public LexLocation getUpdatedFrom()
	{
		if (updatedSomething)
		{
			return updatedFrom;
		}
		else if (outerState != null)
		{
			return outerState.getUpdatedFrom();
		}
		else
		{
			return LexLocation.ANY;
		}
	}

	public LexLocation getUpdatedFrom(TCNameSet names)
	{
		for (TCNameToken name: names)
		{
			if (updatedVariables.containsKey(name))
			{
				return updatedVariables.get(name);
			}
		}
		
		return LexLocation.ANY;
	}
	
	private void didUpdateState(LexLocation from)
	{
		if (outerState != null)
		{
			outerState.didUpdateState(from);
		}
		else
		{
			updatedSomething = true;				// Module level
			updatedFrom = from;
		}
	}

	public void didUpdateState(TCNameToken name, LexLocation from)
	{
		if (locals.contains(name))
		{
			updatedSomething = true;				// A local dcl update
			updatedFrom = name.getLocation();
		}
		else if (outerState != null)
		{
			outerState.didUpdateState(name, from);	// May be an outer* local
		}
		else
		{
			updatedVariables.put(name, from);
			updatedSomething = true;				// A module state update
			updatedFrom = name.getLocation();
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
		locals.add(name);
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

	public void combineWith(POGState state)
	{
		while (state.outerState != null)
		{
			state = state.outerState;	// Find module level
		}
		
		updatedVariables.putAll(state.updatedVariables);
		updatedSomething = updatedSomething || state.updatedSomething;
	}
}
