/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package vdmj.commands;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Interpreter;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import vdmj.DAPDebugReader;
import workspace.Log;

public class InitCommand extends Command
{
	public static final String USAGE = "Usage: init";
	public static final String[] HELP =	{ "init", "init - re-initialize the specification" };
	
	public InitCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		if (parts.length != 1)
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request, boolean wait)
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			LexLocation.clearLocations();
			sb.append("Cleared all coverage information\n");
			DAPDebugReader dbg = null;

			try
			{
				dbg = new DAPDebugReader();
				dbg.start();
				Interpreter.getInstance().init();
			}
			catch (Exception e)
			{
				sb.append("Initialization failed: " + e.getMessage() + "\n");
			}
			finally
			{
				if (dbg != null)
				{
					dbg.interrupt();
				}
			}
			
			sb.append("Global context initialized\n");
			
			return new DAPMessageList(request, new JSONObject("result", sb.toString()));
		}
		catch (Exception e)
		{
			Log.error(e);
			return new DAPMessageList(request, e);
		}
	}
}
