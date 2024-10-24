/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold state information for POG of statements, which involve potentially
 * changing state variables.
 */
public class POGState
{
	private boolean hasUpdatedState;
	private POGState outerState;
	private TCNameList locals;
	
	public POGState()
	{
		hasUpdatedState = false;
		outerState = null;
		locals = new TCNameList();
	}
	
	private POGState(boolean hasUpdatedState, POGState outerState)
	{
		this.hasUpdatedState = hasUpdatedState;
		this.outerState = outerState;
		this.locals = new TCNameList();
	}
	
	@Override
	public String toString()
	{
		return (hasUpdatedState ?  "has" : "has not") + " updated state" +
				(locals.isEmpty() ? "" : Utils.listToString("(locals ", locals, ", ", ")"));
	}
	
	public POGState getCopy()
	{
		return new POGState(hasUpdatedState, outerState);
	}
	
	public POGState getLink()
	{
		return new POGState(false, this);
	}
	
	public boolean hasUpdatedState()
	{
		return hasUpdatedState || (outerState != null && outerState.hasUpdatedState());
	}

	public void setUpdateState(boolean updatedState)
	{
		hasUpdatedState = hasUpdatedState || updatedState;
	}
	
	public void didUpdateState()	// Used by call statements
	{
		if (outerState != null)
		{
			outerState.didUpdateState();
		}
		else
		{
			hasUpdatedState = true;				// Module level
		}
	}

	public void didUpdateState(TCNameToken name)
	{
		if (locals.contains(name))
		{
			hasUpdatedState = true;				// A local dcl update
		}
		else if (outerState != null)
		{
			outerState.didUpdateState(name);	// May be an outer* local
		}
		else
		{
			hasUpdatedState = true;				// A module state update
		}
	}

	public void addDclState(TCNameToken name)
	{
		locals.add(name);
	}
}
