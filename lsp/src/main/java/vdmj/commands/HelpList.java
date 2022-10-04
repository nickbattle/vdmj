/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package vdmj.commands;

import java.util.TreeMap;

/**
 * An ordered list of "command", "usage" help pairs.
 */
public class HelpList extends TreeMap<String, String>
{
	private static final long serialVersionUID = 1L;

	public HelpList(String... lines)
	{
		add(lines);
	}
	
	public HelpList(HelpList list, String... lines)
	{
		add(list);
		add(lines);
	}
	
	private void add(String... lines)
	{
		for (String line: lines)
		{
			String[] parts = line.split("\\s+");
			put(parts[0], line);
		}
	}

	public void add(HelpList list)
	{
		putAll(list);
	}
}
