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
 * 
 * Commands can implement InitRunnable and ScriptRunnable to enable them to be used as
 * the "command" of a launch request, and to be included in "scripts", respectively.
 */
public class ExampleCommand extends Command // implements InitRunnable, ScriptRunnable
{
	public static final String USAGE = "Usage: example <text>";
	public static final String[] HELP = { "example", "example <text> - echo text to the console" };

	private final String line;

	/**
	 * The constructor is passed the entire line that the user typed. The first word on the
	 * line will usually match the name of the class - eg. typing "example one two three" will look
	 * for a class called "ExampleCommand". The rest of the line may contain any command
	 * arguments that are needed. The constructor should parse the line and save any arguments
	 * that it needs. If there is a usage error, the constructor should throw an
	 * IllegalArgumentException with a usage message.
	 */
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

	/**
	 * The run method is invoked after construction, and is passed the DAP "evaluate" request,
	 * which may contain other useful fields. Typically, a command will use the arguments that
	 * it parsed in the constructor here.
	 * 
	 * Text returned in the "result" field will be displayed on stdout on the console.
	 * A DAP response with a false "success" field will display the "message" on stderr.
	 */
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		// return new DAPMessageList(request, false, "Error message...", null);
		return new DAPMessageList(request, new JSONObject("result", "You typed '" + line + "'"));
	}

	/**
	 * If this returns true, this command will not be allowed while the interpreter is evaluating
	 * something else. So for example the PrintCommand returns true, since we cannot have overlapping
	 * evaluations.
	 */
	@Override
	public boolean notWhenRunning()
	{
		return false;
	}
}
