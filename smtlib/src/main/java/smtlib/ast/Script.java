/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package smtlib.ast;

import java.util.Vector;

public class Script extends Vector<Command> implements Source
{
	public Script()
	{
		// Empty (sub)script
	}

	public Script(Command... commands)
	{
		for (Command cmd: commands)
		{
			add(cmd);
		}
	}

	public Script(String title)
	{
		add(new Comment(title));
		add(new SetOption(":produce-models", "true"));
		add(new SetInfo(":smt-lib-version", "2.6"));
		add(new SetLogic("ALL"));
	}

	@Override
	public String toSource()
	{
		StringBuilder sb = new StringBuilder();

		for (Command cmd: this)
		{
			sb.append(cmd.toSource());
			sb.append("\n");
		}

		return sb.toString();
	}
}
