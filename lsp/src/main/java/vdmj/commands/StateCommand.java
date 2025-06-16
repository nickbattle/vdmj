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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package vdmj.commands;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import workspace.Diag;

public class StateCommand extends AnalysisCommand
{
	public static final String USAGE = "Usage: state";
	public static final String HELP = "state - list the state of the current module";
	
	public StateCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("state"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		try
		{
			if (Settings.dialect != Dialect.VDM_SL)
			{
				return new DAPMessageList(request,
						false, "Command only available for VDM-SL", null);			
			}
			
			ModuleInterpreter interpreter = ModuleInterpreter.getInstance();
			Context c = interpreter.getStateContext();
			String result = (c == null ? "(no state)" : c.toString());

			return new DAPMessageList(request, new JSONObject("result", result));
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new DAPMessageList(request, e);
		}
	}

	@Override
	public boolean notWhenRunning()
	{
		return false;
	}
}
