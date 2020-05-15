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

import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.values.Value;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import vdmj.DAPDebugReader;
import workspace.Log;

public class PrintCommand extends Command
{
	private String expression;

	public PrintCommand(String line) throws Exception
	{
		String[] parts = line.split("\\s+", 2);
		
		if (parts.length == 2)
		{
			this.expression = parts[1];
		}
		else
		{
			usage();
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
			Value result = Interpreter.getInstance().execute(expression);
			long after = System.currentTimeMillis();
			
			String answer = "= " + result + "\nExecuted in " + (double)(after-before)/1000 + " secs.\n";
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

	@Override
	protected void usage() throws Exception
	{
		throw new Exception("Usage: print <expression>");
	}

	public static String[] help()
	{
		return new String[] { "print", "print <exp> - evaluate an expression" };
	}
}
