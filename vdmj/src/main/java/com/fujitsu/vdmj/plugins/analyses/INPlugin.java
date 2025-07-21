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

package com.fujitsu.vdmj.plugins.analyses;

import static com.fujitsu.vdmj.plugins.PluginConsole.fail;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INStatementList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.AnalysisEvent;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.events.AbstractCheckFilesEvent;
import com.fujitsu.vdmj.plugins.events.CheckCompleteEvent;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.runtime.Interpreter;

/**
 * IN analysis plugin
 */
abstract public class INPlugin extends AnalysisPlugin implements EventListener
{
	protected boolean startInterpreter;		// eg. -e or -remote as well as -i
	protected boolean interactive;			// eg. -i or -simulation
	protected String defaultName;
	protected String logfile;
	
	@Override
	public String getName()
	{
		return "IN";
	}
	
	@Override
	public int getPriority()
	{
		return IN_PRIORITY;
	}

	@Override
	public void init()
	{
		startInterpreter = false;
		interactive = false;
		defaultName = null;
		logfile = null;
		
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckCompleteEvent.class, this);
	}

	public static INPlugin factory(Dialect dialect) throws Exception
	{
		switch (dialect)
		{
			case VDM_SL:
				return new INPluginSL();
				
			case VDM_PP:
				return new INPluginPP();
				
			case VDM_RT:
				return new INPluginRT();
				
			default:
				throw new Exception("Unknown dialect: " + dialect);
		}
	}
	
	@Override
	public void usage()
	{
		println("-default <name>: set the default module/class");
		println("-pre: disable precondition checks");
		println("-post: disable postcondition checks");
		println("-inv: disable type/state invariant checks");
		println("-dtc: disable all dynamic type checking");
		println("-exceptions: raise pre/post/inv violations as <RuntimeError>");
		println("-measures: disable recursive measure checking");
		println("-log <filename>: enable real-time event logging");
	}
	
	@Override
	public void processArgs(List<String> argv)
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			switch(iter.next())
			{
				// The first five are also processed in CMDPlugin, so we
				// don't remove them here, just skip any arguments.

				case "-i":
					setStartInterpreter();
					interactive = true;
					break;
					
				case "-e":
					setStartInterpreter();
					interactive = false;
					if (iter.hasNext()) iter.next();	// skip arg
					break;

				case "-cmd":
					setStartInterpreter();
					interactive = false;
					if (iter.hasNext()) iter.next();	// skip arg
					break;

	    		case "-remote":
					setStartInterpreter();
	    			interactive = false;
					if (iter.hasNext()) iter.next();	// skip arg
	    			break;
	    			
	    		case "-simulation":
					setStartInterpreter();
	    			interactive = true;
					if (iter.hasNext()) iter.next();	// skip arg
					break;

				// The remaining arguments are processed here and removed

				case "-default":
					iter.remove();
					
					if (iter.hasNext())
					{
						defaultName = iter.next();
						iter.remove();
					}
					else
					{
						fail("-default requires a name");
					}
					break;
					
				case "-log":
					iter.remove();
					
					if (iter.hasNext())
					{
						logfile = iter.next();
						iter.remove();
					}
					else
					{
						fail("-log requires a log file name");
					}
					break;
					
	    		case "-pre":
	    			iter.remove();
	    			Settings.prechecks = false;
	    			break;

	    		case "-post":
	    			iter.remove();
	    			Settings.postchecks = false;
	    			break;
	    			
	    		case "-inv":
	    			iter.remove();
	    			Settings.invchecks = false;
	    			break;
	    			
	    		case "-dtc":
	    			iter.remove();
	    			// NB. Turn off both when no DTC
	    			Settings.invchecks = false;
	    			Settings.dynamictypechecks = false;
	    			break;
	    			
	    		case "-exceptions":
	    			iter.remove();
	    			Settings.exceptions = true;
	    			break;
	    			
	    		case "-measures":
	    			iter.remove();
	    			Settings.measureChecks = false;
	    			break;
			}
		}
		
		if (logfile != null && Settings.dialect != Dialect.VDM_RT)
		{
			fail("The -log option can only be used with -vdmrt");
		}
	}
	
	private void setStartInterpreter()
	{
		if (startInterpreter)	// Already set?
		{
			fail("Only one of: -i, -e, -cmd, -remote, -simulation");
		}

		startInterpreter = true;
	}
	
	@Override
	public List<VDMMessage> handleEvent(AnalysisEvent event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			CheckPrepareEvent pevent = (CheckPrepareEvent)event;
			
			if (pevent.getFiles().isEmpty() && !interactive)
			{
				fail("You did not identify any source files");
			}

			return interpreterPrepare();
		}
		else if (event instanceof CheckCompleteEvent)
		{
			if (startInterpreter)
			{
				event.setProperty(AbstractCheckFilesEvent.TITLE, "Initialized");
				event.setProperty(AbstractCheckFilesEvent.KIND, "init");
				return interpreterInit();
			}
			else
			{
				return null;
			}
		}
		else
		{
			throw new Exception("Unhandled event: " + event);
		}
	}
	
	abstract protected List<VDMMessage> interpreterPrepare();

	abstract protected List<VDMMessage> interpreterInit();

	abstract public <T extends Interpreter> T getInterpreter();

	abstract public <T extends Collection<?>> T getIN();
	
	abstract public INExpressionList findExpressions(File file, int lineno);
	
	abstract public INStatementList findStatements(File file, int lineno);
}
