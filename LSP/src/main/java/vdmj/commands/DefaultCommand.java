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

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import workspace.Log;

public class DefaultCommand extends Command
{
	private String defaultName;

	public DefaultCommand(String line) throws Exception
	{
		String[] parts = line.split("\\s+");
		
		if (parts.length == 2)
		{
			this.defaultName = parts[1];
		}
		else
		{
			usage();
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
			Log.error(e);
			return new DAPMessageList(request, e);
		}
	}

	@Override
	protected void usage() throws Exception
	{
		throw new Exception("Usage: default <default module or class>");
	}

	public static String[] help()
	{
		return new String[]	{ "default", "default <name> - set the default class or module name" };
	}
}
