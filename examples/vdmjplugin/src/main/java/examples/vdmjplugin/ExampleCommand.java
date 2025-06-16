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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
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
	
	protected ExampleCommand(String line, ExamplePlugin plugin)
	{
		super(line);
		this.plugin = plugin;
	}

	@Override
	public String run(String line)
	{
		if (argv.length != 2)
		{
			return "Usage: maxlen <length>";
		}
		
		try
		{
			int len = Integer.parseInt(argv[1]);
			plugin.setMaxlen(len);
			
			for (VDMMessage m: plugin.checkDefinitions())
			{
				println(m);
			}
			
			return null;
		}
		catch (NumberFormatException e)
		{
			return "Usage: maxlen <length>";
		}
	}
}
