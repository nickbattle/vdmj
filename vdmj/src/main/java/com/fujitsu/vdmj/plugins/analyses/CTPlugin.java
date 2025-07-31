/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package com.fujitsu.vdmj.plugins.analyses;

import java.util.List;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.AnalysisEvent;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.HelpList;
import com.fujitsu.vdmj.plugins.commands.RuntraceCommand;

/**
 * CT combinatorial testing plugin
 */
public class CTPlugin extends AnalysisPlugin implements EventListener
{
	@Override
	public String getName()
	{
		return "CT";
	}
	
	@Override
	public int getPriority()
	{
		return CT_PRIORITY;
	}

	@Override
	public void init()
	{
		// Only supplies Commands.
	}

	public static CTPlugin factory(Dialect dialect) throws Exception
	{
		return new CTPlugin();
	}
	
	@Override
	public void usage()
	{
		// No contribution
	}
	
	@Override
	public void processArgs(List<String> argv)
	{
		// Nothing for CT
	}
	
	@Override
	public List<VDMMessage> handleEvent(AnalysisEvent event) throws Exception
	{
		throw new Exception("Unhandled event: " + event);
	}

	@Override
	public AnalysisCommand getCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		switch (parts[0])
		{
			case "runtrace":
			case "rt":
			case "debugtrace":
			case "dt":
			case "runalltraces":
			case "savetrace":
			case "seedtrace":
			case "filter":
				return new RuntraceCommand(line);

			default:
				return null;
		}
	}
	
	@Override
	public HelpList getCommandHelp()
	{
		return RuntraceCommand.help();
	}
}
