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

package examples;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import vdmj.commands.Command;

/**
 * Example of how to implement a Command for the example plugin.
 */
public class ExampleCommand extends Command
{
	public static final String USAGE = "Usage: example <text>";
	public static final String[] HELP = { "example", "example <text> - echo text to the console" };

	private final String line;

	public ExampleCommand(String line)
	{
		String[] parts = line.split("\\s+", 2);
		
		if (parts.length == 2)
		{
			this.line = parts[1];
		}
		else
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public DAPMessageList run(DAPRequest request)
	{
		// Do whatever you like with request...
		return new DAPMessageList(request, new JSONObject("result", "You typed '" + line + "'"));
	}

	@Override
	public boolean notWhenRunning()
	{
		return false;
	}
}
