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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.traces;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * A class representing a trace definition.
 */
public class TCTraceRepeatDefinition extends TCTraceDefinition
{
    private static final long serialVersionUID = 1L;
	public final TCTraceCoreDefinition core;
	public final long from;
	public final long to;

	public TCTraceRepeatDefinition(LexLocation location, TCTraceCoreDefinition core, long from, long to)
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
	public void typeCheck(Environment base, NameScope scope)
	{
		Environment local = base;
		core.typeCheck(local, scope);

		if (from > to)
		{
			TypeChecker.report(3277, "Trace repeat illegal values", location);
		}
	}
}
