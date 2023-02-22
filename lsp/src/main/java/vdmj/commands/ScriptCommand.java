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

import java.io.IOException;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.ScriptExecutor;

public class ScriptCommand extends AnalysisCommand implements ScriptRunnable
{
	public static final String USAGE = "Usage: script <file>";
	public static final String HELP = "script <file> - run commands from file";
	
	private final String filename;
	
	public ScriptCommand(String line)
	{
		super(line);
		
		if (argv.length == 2)
		{
			filename = argv[1];
		}
		else
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public DAPMessageList run(DAPRequest request)
	{
		ScriptExecutor executor = new ScriptExecutor("script", request, filename);
		executor.start();
		return null;
	}
	
	@Override
	public boolean notWhenRunning()
	{
		return true;
	}

	@Override
	public String scriptRun(DAPRequest request) throws IOException
	{
		return "Cannot nest scripts";
	}
}
