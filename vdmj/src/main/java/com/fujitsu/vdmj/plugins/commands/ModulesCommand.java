/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package com.fujitsu.vdmj.plugins.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.analyses.TCPlugin;
import com.fujitsu.vdmj.plugins.analyses.TCPluginSL;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

public class ModulesCommand extends AnalysisCommand
{
	private final static String KIND = Settings.dialect == Dialect.VDM_SL ? "modules" : "classes";

	public ModulesCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals(KIND))
		{
			throw new IllegalArgumentException(KIND);
		}
	}

	@Override
	public void run()
	{
		if (argv.length != 1)
		{
			println(KIND);
			return;
		}

		TCPlugin tc = registry.getPlugin("TC");	// NB. TC has DEFAULTs combined
		String def = Interpreter.getInstance().getDefaultName();
		
		if (tc instanceof TCPluginSL)
		{
			TCModuleList list = tc.getTC();
	
			for (TCModule module: list)
			{
				println(module.name.getName() + (module.name.getName().equals(def) ? " (default)" : ""));
			}
		}
		else
		{
			TCClassList list = tc.getTC();
			
			for (TCClassDefinition clazz: list)
			{
				println(clazz.name.getName() + (clazz.name.getName().equals(def) ? " (default)" : ""));
			}
		}
	}
	
	public static void help()
	{
		printf("%s - list the specification %s\n", KIND, KIND);
	}
}
