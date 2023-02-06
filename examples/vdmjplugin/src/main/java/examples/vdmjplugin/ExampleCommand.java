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

package examples.vdmjplugin;

import static com.fujitsu.vdmj.plugins.PluginConsole.*;

import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.AnalysisCommand;

public class ExampleCommand extends AnalysisCommand
{
	private final ExamplePlugin plugin;
	
	protected ExampleCommand(String[] argv, ExamplePlugin plugin)
	{
		super(argv);
		this.plugin = plugin;
	}

	@Override
	public void run()
	{
		if (argv.length != 2)
		{
			println("Usage: maxlen <length>");
			return;
		}
		
		try
		{
			int len = Integer.parseInt(argv[1]);
			plugin.setMaxlen(len);
			
			for (VDMMessage m: plugin.checkDefinitions())
			{
				println(m);
			}
		}
		catch (NumberFormatException e)
		{
			println("Usage: maxlen <length>");
		}
	}
}
