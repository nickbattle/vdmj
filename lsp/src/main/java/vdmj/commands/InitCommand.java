/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

import com.fujitsu.vdmj.lex.LexLocation;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import dap.InitExecutor;
import json.JSONObject;
import workspace.plugins.DAPPlugin;

public class InitCommand extends AnalysisCommand implements ScriptRunnable
{
	public static final String USAGE = "Usage: init";
	public static final String HELP = "init - re-initialize the specification";
	
	public InitCommand(String line)
	{
		super(line);
		
		if (argv.length != 1)
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		InitExecutor exec = new InitExecutor("init", request, null, null)
		{
			@Override
			protected void head() throws IOException
			{
				// No header
			}
			
			@Override
			protected void tail(double time) throws IOException
			{
				String output =
					"Global context initialized in " + time + " secs.\n" +
					"Cleared all coverage information";
				server.writeMessage(new DAPResponse(request, true, null, new JSONObject("result", output)));
			}

			@Override
			protected void error(Throwable e) throws IOException
			{
				server.writeMessage(new DAPResponse(request, false, e.getMessage(), null));
				server.stdout("Init terminated.");
			}
		};
		
		exec.start();
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
		LexLocation.clearLocations();
		DAPPlugin.getInstance().getInterpreter().init();
		return "()\nGlobal context initialized\nCleared all coverage information";
	}
}
