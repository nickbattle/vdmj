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

/**
 * A class to hold state information for POG of statements, which involve potentially
 * changing state variables.
 */
public class POGState
{
	private boolean hasUpdatedState;
	private boolean hasReadState;
	
	public POGState()
	{
		hasUpdatedState = false;
	}
	
	private POGState(boolean hasUpdatedState, boolean hasReadState)
	{
		this.hasUpdatedState = hasUpdatedState;
		this.hasReadState = hasReadState;
	}
	
	public POGState getCopy()
	{
		return new POGState(hasUpdatedState, hasReadState);
	}
	
	public boolean hasUpdatedState()
	{
		return hasUpdatedState;
	}
	
	public boolean hasReadState()
	{
		return hasReadState;
	}

	public void didUpdateState(boolean flag)
	{
		hasUpdatedState = hasUpdatedState || flag;
	}

	public void didReadState(boolean flag)
	{
		hasReadState = hasReadState || flag;
	}
}
