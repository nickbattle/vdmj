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

import com.fujitsu.vdmj.runtime.Interpreter;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import workspace.Diag;

public class DefaultCommand extends AnalysisCommand
{
	public static final String USAGE = "Usage: default <default module or class>";
	public static final String HELP = "default <name> - set the default class or module name";
	
	private String defaultName;

	public DefaultCommand(String line)
	{
		super(line);
		
		if (argv.length == 2)
		{
			this.defaultName = argv[1];
		}
		else
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		try
		{
			Interpreter.getInstance().setDefaultName(defaultName);
			return new DAPMessageList(request,
					new JSONObject("result", "Default name changed to " + defaultName));
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
		return true;
	}
}
