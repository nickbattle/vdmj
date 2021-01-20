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

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONArray;
import json.JSONObject;
import vdmj.DAPDebugReader;
import workspace.DAPWorkspaceManager;
import workspace.Log;

public class RuntraceCommand extends Command
{
	public static final String USAGE = "Usage: runtrace <trace> <number>";
	public static final String[] HELP = { "runtrace", "runtrace <trace> <number> - run one test from a trace" };
	
	public final String tracename;
	public final long testNumber;

	public RuntraceCommand(String line)
	{
		String[] parts = line.split("\\s+", 3);
		
		if (parts.length == 3)
		{
			this.tracename = parts[1];
			this.testNumber = Long.parseLong(parts[2]);
		}
		else
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		DAPDebugReader dbg = null;
		
		try
		{
			dbg = new DAPDebugReader();
			dbg.start();
			
			long before = System.currentTimeMillis();
			JSONObject result = DAPWorkspaceManager.getInstance().ctRuntrace(request, tracename, testNumber);
			long after = System.currentTimeMillis();
			
			String answer = display(result) + "\nExecuted in " + (double)(after-before)/1000 + " secs.\n";
			return new DAPMessageList(request, new JSONObject("result", answer, "variablesReference", 0));
		}
		catch (Exception e)
		{
			Log.error(e);
			return new DAPMessageList(request, e);
		}
		finally
		{
			if (dbg != null)
			{
				dbg.interrupt();	// Stop the debugger reader.
			}
		}
	}

	public String display(JSONObject result)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Test ");
		sb.append(result.get("id").toString());
		sb.append(" = ");
		
		JSONArray sequence = result.get("sequence");
		String sep = "";
		
		for (int i=0; i<sequence.size(); i++)
		{
			JSONObject pair = sequence.index(i);
			String call = pair.get("case");
			sb.append(sep);
			sb.append(call);
			sep = ", ";
		}
		
		sb.append("\nResult = [");
		sep = "";
		
		for (int i=0; i<sequence.size(); i++)
		{
			JSONObject pair = sequence.index(i);
			String value = pair.get("result");
			sb.append(sep);
			sb.append(value);
			sep = ", ";
		}
		
		sb.append("] ");
		long verdict = result.get("verdict");
		
		switch ((int)verdict)
		{
			case 1:		sb.append("PASSED"); break;
			case 2:		sb.append("FAILED"); break;
			case 3:		sb.append("INCONCLUSIVE"); break;
			case 4:		sb.append("SKIPPED"); break;

			default:	sb.append("UNKNOWN"); break;
		}
		
		return sb.toString();
	}
}
