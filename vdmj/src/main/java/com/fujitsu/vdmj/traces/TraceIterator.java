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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.traces;

import com.fujitsu.vdmj.in.traces.INTraceVariableList;

public abstract class TraceIterator
{
	private INTraceVariableList variables = null;
	
	public void setVariables(INTraceVariableList inTraceVariableList)
	{
		if (variables == null)
		{
			variables = inTraceVariableList;
		}
		else
		{
			// Variables might not be null if there are nested "let" traces that
			// add variables to the same iterator. New items are added at the start,
			// because older (deeper) items can override them.
			variables.addAll(0, inTraceVariableList);
		}
	}

	public CallSequence getVariables()
	{
		return (variables == null) ? new CallSequence() : variables.getVariables();
	}

	@Override
	abstract public String toString();

	abstract public boolean hasMoreTests();

	abstract public CallSequence getNextTest();

	abstract public int count();
	
	abstract public void reset();
}
