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

import static com.fujitsu.vdmj.plugins.PluginConsole.fail;
import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static com.fujitsu.vdmj.plugins.PluginConsole.validateCharset;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.CommandList;
import com.fujitsu.vdmj.plugins.AnalysisEvent;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.plugins.events.CheckSyntaxEvent;

/**
 * AST analysis plugin
 */
abstract public class ASTPlugin extends AnalysisPlugin implements EventListener
{
	protected List<File> files;
	protected Charset filecharset;
	
	@Override
	public String getName()
	{
		return "AST";
	}

	@Override
	public void init()
	{
		files = new Vector<File>();
		filecharset = Charset.defaultCharset();
		
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckSyntaxEvent.class, this);
	}

	public static ASTPlugin factory(Dialect dialect) throws Exception
	{
		switch (dialect)
		{
			case VDM_SL:
				return new ASTPluginSL();
				
			case VDM_PP:
				return new ASTPluginPP();
				
			case VDM_RT:
				return new ASTPluginRT();
				
			default:
				throw new Exception("Unknown dialect: " + dialect);
		}
	}
	
	public void setFiles(List<File> files)
	{
		this.files = files;
	}

	public List<File> getFiles()
	{
		return files;
	}
	
	public Charset getFileCharset()
	{
		return filecharset;
	}
	
	public void checkForUpdates(long timestamp)
	{
		for (File file: files)
		{
			if (file.lastModified() > timestamp)
			{
				printf("File %s has changed\n", file);
			}
		}
	}
	
	@Override
	public void usage()
	{
		println("-c <charset>: select a file charset");
	}

	@Override
	public void processArgs(List<String> argv)
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			switch (iter.next())
			{
				case "-c":
	    			iter.remove();
	    			
	    			if (iter.hasNext())
	    			{
	    				filecharset = validateCharset(iter.next());
	    				iter.remove();
	    			}
	    			else
	    			{
	    				fail("-c option requires a charset name");
	    			}
	    			break;
    		}
		}
	}
	
	@Override
	public List<VDMMessage> handleEvent(AnalysisEvent event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			return syntaxPrepare();
		}
		else if (event instanceof CheckSyntaxEvent)
		{
			return syntaxCheck();
		}
		else
		{
			throw new Exception("Unhandled event: " + event);
		}
	}

	abstract protected List<VDMMessage> syntaxPrepare();

	abstract protected List<VDMMessage> syntaxCheck();

	abstract public <T extends Mappable> T getAST();
	
	abstract public int getCount();
	
	protected CommandList commandList = new CommandList
	(
		// None yet
	);
	
	@Override
	public AnalysisCommand getCommand(String[] argv)
	{
		return lookup(argv, commandList);
	}
	
	@Override
	public void help()
	{
		showHelp(commandList);
	}
}
