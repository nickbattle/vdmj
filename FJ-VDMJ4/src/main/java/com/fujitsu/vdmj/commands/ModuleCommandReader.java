/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.commands;

import java.util.List;

import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.in.modules.INModule;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;

/**
 * A class to read and perform module related commands from standard input.
 */
public class ModuleCommandReader extends CommandReader
{
	/** A ModuleInterpreter version of the interpreter field. */
	private final ModuleInterpreter minterpreter;

	public ModuleCommandReader(ModuleInterpreter interpreter, String prompt)
	{
		super(interpreter, prompt);
		minterpreter = interpreter;
	}

	@Override
	protected boolean doModules(String line)
	{
		String def = minterpreter.getDefaultName();
		List<INModule> modules = minterpreter.getModules();

		for (INModule m: modules)
		{
			if (m.name.getName().equals(def))
			{
				println(m.name.getName() + " (default)");
			}
			else
			{
				println(m.name.getName());
			}
		}

		return true;
	}

	@Override
	protected boolean doState(String line)
	{
		Context c = minterpreter.getStateContext();
		print(c == null ? "(no state)\n" : c.toString());
		return true;
	}

	@Override
	protected boolean doDefault(String line) throws Exception
	{
		String parts[] = line.split("\\s+");

		if (parts.length != 2)
		{
			throw new Exception("Usage: default <default module name>");
		}

		minterpreter.setDefaultName(parts[1]);
		println("Default module set to " + minterpreter.getDefaultName());
		return true;
	}
	
	@Override
	protected boolean doAllTraces(String line)
	{
		int index = line.indexOf(' ') + 1;
		String pattern = (index == 0) ? ".*" : line.substring(index);

		for (INModule m: minterpreter.getModules())
		{
			if (m.name.getName().matches(pattern))
			{
    			for (INDefinition d: m.defs)
    			{
    				if (d instanceof INNamedTraceDefinition)
    				{
    					String cmd = "runtrace " + d.name.getExplicit(true);
    					println("-------------------------------------");
    					println(cmd);
    					doRuntrace(cmd, false);
    				}
    			}
			}
		}
		
		return true;
	}

	@Override
	protected void doHelp(String line)
	{
		println("modules - list the loaded module names");
		println("default <module> - set the default module name");
		println("state - show the default module state");
		super.doHelp(line);
	}
}
