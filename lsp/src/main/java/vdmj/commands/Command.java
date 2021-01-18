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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import json.JSONObject;
import workspace.Log;

abstract public class Command
{
	public static Command parse(String line)
	{
		if (line == null || line.isEmpty())
		{
			return new NullCommand();
		}

		String[] parts = line.split("\\s+");
		String name = parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1).toLowerCase();
		
		try
		{
			Log.printf("Trying to load command vdmj.commands.%sCommand", name);
			Class<?> clazz = Class.forName("vdmj.commands." + name + "Command");
			Constructor<?> ctor = clazz.getConstructor(String.class); 
			return (Command)ctor.newInstance(line);
		}
		catch (ClassNotFoundException e)
		{
			Log.error(e);
			return new ErrorCommand("Unknown command '" + name.toLowerCase() + "'. Try help");
		}
		catch (InvocationTargetException e)
		{
			Log.error(e.getTargetException());
			
			if (e.getTargetException() instanceof IllegalArgumentException)
			{
				return new ErrorCommand(e.getTargetException().getMessage());
			}
			else
			{
				return new ErrorCommand("Error: " + e.getTargetException().getMessage());
			}
		}
		catch (Exception e)
		{
			Log.error(e);
			return new ErrorCommand("Error: " + e.getMessage());
		}
	}

	public abstract DAPMessageList run(DAPRequest request, boolean wait);
	
	protected DAPResponse stdout(String message)
	{
		return new DAPResponse("output", new JSONObject("output", message));
	}
}
