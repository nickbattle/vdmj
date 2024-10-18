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

import java.util.Vector;

/**
 * A list of POG states for the sub-clauses in a statements. These are combined when
 * the execution paths rejoin.
 */
public class POGStateList extends Vector<POGState>
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Add a clone of the parent and return it for use in subclauses.
	 */
	public POGState addCopy(POGState parent)
	{
		POGState newState = parent.getCopy();
		this.add(newState);
		return newState;
	}
	
	/**
	 * Combine all of the subclause states and update the parent.
	 */
	public void combineInto(POGState parent)
	{
		boolean hasUpdatedState = false;
		boolean hasReadState = false;
		
		for (POGState state: this)
		{
			hasUpdatedState = hasUpdatedState || state.hasUpdatedState();
			hasReadState = hasReadState || state.hasReadState();
		}
		
		parent.didUpdateState(hasUpdatedState);
		parent.didReadState(hasReadState);
	}
}
