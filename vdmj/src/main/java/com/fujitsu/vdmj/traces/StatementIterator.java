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

import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Breakpoint;

public class StatementIterator extends TraceIterator
{
	public final INStatement statement;

	private boolean done = false;
	
	private static final LexLocation NOWHERE = new LexLocation();
	private static final Breakpoint DUMMY = new Breakpoint(NOWHERE);

	public StatementIterator(INStatement newStatement)
	{
		this.statement = newStatement;
		statement.breakpoint = DUMMY;	// Reduce garbage
	}

	@Override
	public String toString()
	{
		return statement.toString();
	}

	@Override
	public CallSequence getNextTest()
	{
		CallSequence test = getVariables();
		test.add(statement);
		done = true;
		return test;
	}

	@Override
	public boolean hasMoreTests()
	{
		return !done;
	}

	@Override
	public int count()
	{
		return 1;
	}

	@Override
	public void reset()
	{
		done = false;
	}
}
