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
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static com.fujitsu.vdmj.plugins.PluginConsole.validateCharset;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.plugins.events.CheckSyntaxEvent;
import com.fujitsu.vdmj.plugins.events.Event;

/**
 * AST analysis plugin
 */
abstract public class ASTPlugin extends AnalysisPlugin implements EventListener
{
	protected List<File> files;
	protected Charset filecharset;
	protected List<VDMError> errors;
	protected List<VDMWarning> warnings;
	protected boolean nowarn;
	
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
		errors = new Vector<VDMError>();
		warnings = new Vector<VDMWarning>();
		nowarn = false;
		
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
	
	@Override
	public void usage()
	{
		println("-c <charset>: select a file charset");
	}
	
	public List<File> getFiles()
	{
		return files;
	}
	
	@Override
	public void processArgs(List<String> argv)
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			String arg = iter.next();
			
			if (arg.equals("-w"))
			{
				nowarn = true;	// Removed in TC
			}
    		else if (arg.equals("-c"))
    		{
    			iter.remove();
    			
    			if (iter.hasNext())
    			{
    				filecharset = validateCharset(iter.next());
    			}
    			else
    			{
    				fail("-c option requires a charset name");
    			}
    		}
		}
	}
	
	public void setFiles(List<File> files)
	{
		this.files = files;
	}

	@Override
	public <T> T handleEvent(Event event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			errors.clear();
			warnings.clear();
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

	abstract protected <T> T syntaxPrepare();

	abstract protected <T> T syntaxCheck();

	abstract public <T extends Mappable> T getAST();
}
