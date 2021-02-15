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


public class SequenceIterator extends TraceIterator
{
	private TraceIteratorList nodes;

	public SequenceIterator()
	{
		this.nodes = new TraceIteratorList();
	}
	
	public SequenceIterator(TraceIteratorList nodes)
	{
		this.nodes = nodes;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		String sep = "";

		for (TraceIterator node: nodes)
		{
			sb.append(sep);
			sb.append(node.toString());
			sep = "; ";
		}

		return sb.toString();
	}

	@Override
	public CallSequence getNextTest()
	{
		CallSequence test = getVariables();
		test.addAll(nodes.getNextTestSequence());
		return test;
	}

	@Override
	public boolean hasMoreTests()
	{
		return nodes.hasMoreSequenceTests();
	}

	@Override
	public int count()
	{
		return nodes.countSequence();
	}

	@Override
	public void reset()
	{
		nodes.reset();
	}
}
