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

package com.fujitsu.vdmj.plugins.analyses;

import static com.fujitsu.vdmj.plugins.PluginConsole.plural;
import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.AnalysisEvent;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.CommandList;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.HelpList;
import com.fujitsu.vdmj.plugins.commands.PogCommand;
import com.fujitsu.vdmj.plugins.events.AbstractCheckFilesEvent;
import com.fujitsu.vdmj.plugins.events.CheckCompleteEvent;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.pog.ProofObligationList;

/**
 * PO analysis plugin
 */
abstract public class POPlugin extends AnalysisPlugin implements EventListener
{
	private boolean poAtStart;

	@Override
	public String getName()
	{
		return "PO";
	}
	
	@Override
	public int getPriority()
	{
		return PO_PRIORITY;
	}

	@Override
	public void init()
	{
		poAtStart = false;
		
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckCompleteEvent.class, this);
	}

	public static POPlugin factory(Dialect dialect) throws Exception
	{
		switch (dialect)
		{
			case VDM_SL:
				return new POPluginSL();
				
			case VDM_PP:
				return new POPluginPP();
				
			case VDM_RT:
				return new POPluginRT();
				
			default:
				throw new Exception("Unknown dialect: " + dialect);
		}
	}
	
	@Override
	public void usage()
	{
		println("-p: generate proof obligations");
	}
	
	@Override
	public void processArgs(List<String> argv)
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			switch (iter.next())
			{
				case "-p":
	    			iter.remove();
	    			poAtStart = true;
	    			break;
    		}
		}
	}

	@Override
	public List<VDMMessage> handleEvent(AnalysisEvent event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			return pogPrepare();
		}
		else if (event instanceof CheckCompleteEvent)
		{
			List<VDMMessage> messages = pogGenerate();

			if (poAtStart)
			{
				event.setProperty(AbstractCheckFilesEvent.TITLE, "Analysed");
				event.setProperty(AbstractCheckFilesEvent.KIND, "POG");
				ProofObligationList list = getProofObligations();
				
				if (list.isEmpty())
				{
					println("No proof obligations generated");
				}
				else
				{
					println("Generated " + plural(list.size(), "proof obligation", "s") + ":\n");
					printf("%s", list.toString());
				}
			}
			
			return messages;
		}
		else
		{
			throw new Exception("Unhandled event: " + event);
		}
	}

	abstract protected List<VDMMessage> pogPrepare();

	abstract protected List<VDMMessage> pogGenerate();
	
	abstract public ProofObligationList getProofObligations();

	abstract public <T extends Collection<?>> T getPO();

	protected CommandList commandList = new CommandList
	(
		PogCommand.class
	);
	
	@Override
	public AnalysisCommand getCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		switch (parts[0])
		{
			case "pog":		return new PogCommand(line);

			default:
				return null;
		}
	}
	
	@Override
	public HelpList getCommandHelp()
	{
		return new HelpList(PogCommand.help());
	}
}
