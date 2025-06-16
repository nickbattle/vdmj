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

package plugins;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.HelpList;

import commands.TranslateCommand;

/**
 * A VDMJ Plugin to enable the V2C translate features. This is a simple plugin which
 * provides a single "translate" command via the getCommand method.
 */
public class V2CPluginVDMJ extends AnalysisPlugin
{
	public static AnalysisPlugin factory(Dialect dialect)
	{
		return new V2CPluginVDMJ();		// For all dialects. This could be specialized.
	}
	
	@Override
	public String getName()
	{
		return "V2C";
	}

	@Override
	public void init()
	{
		// Everything done via getCommand()
	}

	@Override
	public AnalysisCommand getCommand(String line)
	{
		String[] argv = line.split("\\s+");
		
		if (argv[0].equals("translate"))
		{
			return new TranslateCommand(line);
		}
		
		return null;
	}
	
	@Override
	public HelpList getCommandHelp()
	{
		return new HelpList(TranslateCommand.HELP);
	}
}
