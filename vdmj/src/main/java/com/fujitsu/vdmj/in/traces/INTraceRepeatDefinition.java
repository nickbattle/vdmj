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

package com.fujitsu.vdmj.in.traces;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.traces.RepeatIterator;
import com.fujitsu.vdmj.traces.TraceIterator;

/**
 * A class representing a trace definition.
 */
public class INTraceRepeatDefinition extends INTraceDefinition
{
    private static final long serialVersionUID = 1L;
	public final INTraceCoreDefinition core;
	public final long from;
	public final long to;

	public INTraceRepeatDefinition(LexLocation location, INTraceCoreDefinition core, long from, long to)
	{
		super(location);
		this.core = core;
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString()
	{
		return core +
			((from == 1 && to == 1) ? "" :
				(from == to) ? ("{" + from + "}") :
					("{" + from + ", " + to + "}"));
	}

	@Override
	public TraceIterator getIterator(Context ctxt)
	{
		TraceIterator body = core.getIterator(ctxt);

		if (from == 1 && to == 1)
		{
			return body;
		}
		else
		{
			return new RepeatIterator(body, from, to);
		}
	}
}
