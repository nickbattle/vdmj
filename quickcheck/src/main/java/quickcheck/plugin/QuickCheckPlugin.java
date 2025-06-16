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

package quickcheck.plugin;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.HelpList;
import com.fujitsu.vdmj.util.Utils;

import quickcheck.commands.QCRunCommand;
import quickcheck.commands.QuickCheckCommand;

public class QuickCheckPlugin extends AnalysisPlugin
{
	public static AnalysisPlugin factory(Dialect dialect)
	{
		return new QuickCheckPlugin();
	}
	
	@Override
	public String getName()
	{
		return "QC";
	}

	@Override
	public void init()
	{
		// Get everything from PO?
	}
	
	@Override
	public AnalysisCommand getCommand(String line)
	{
		String[] argv = Utils.toArgv(line);
		
		switch (argv[0])
		{
			case "quickcheck":
			case "qc":
				return new QuickCheckCommand(line);
				
			case "qcrun":
			case "qr":
				return new QCRunCommand(line);
		}
		
		return null;
	}
	
	@Override
	public HelpList getCommandHelp()
	{
		return new HelpList(
			QuickCheckCommand.HELP,
			QCRunCommand.HELP
		);
	}
}
