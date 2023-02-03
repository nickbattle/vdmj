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

import java.util.Iterator;
import java.util.List;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.RemoteControl;
import com.fujitsu.vdmj.RemoteSimulation;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.commands.PrintCommand;
import com.fujitsu.vdmj.plugins.events.CheckCompleteEvent;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.plugins.events.Event;

/**
 * IN analysis plugin
 */
abstract public class INPlugin extends AnalysisPlugin implements EventListener
{
	protected boolean startInterpreter;		// eg. -e or -remote as well as -i
	protected boolean interactive;			// eg. -i or -simulation
	protected String defaultName;
	protected String expression;
	protected String logfile;
	protected String remoteControlName;
	protected String remoteSimulationName;
	protected Class<RemoteControl> remoteClass;
	protected Class<RemoteSimulation> remoteSimulation;
	
	@Override
	public String getName()
	{
		return "IN";
	}

	@Override
	public void init()
	{
		startInterpreter = false;
		interactive = false;
		defaultName = null;
		expression = null;
		logfile = null;
		remoteControlName = null;
		remoteSimulationName = null;
		remoteClass = null;
		remoteSimulation = null;
		
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
		println("-i: run the interpreter if successfully type checked");
		println("-e <exp>: evaluate <exp> and stop");
		println("-p: generate proof obligations and stop");
		println("-c <charset>: select a file charset");
		println("-default <name>: set the default module/class");
		println("-pre: disable precondition checks");
		println("-post: disable postcondition checks");
		println("-inv: disable type/state invariant checks");
		println("-dtc: disable all dynamic type checking");
		println("-exceptions: raise pre/post/inv violations as <RuntimeError>");
		println("-measures: disable recursive measure checking");
		println("-annotations: enable annotation processing");
		println("-log <filename>: enable real-time event logging");
		println("-remote <class>: enable remote control");
		println("-simulation <class>: enable simulation control");
	}
	
	@Override
	public void processArgs(List<String> argv)
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			switch(iter.next())
			{			
				case "-i":
					iter.remove();
					startInterpreter = true;
					interactive = true;
					break;
					
				case "-e":
					iter.remove();
					startInterpreter = true;
					interactive = false;
					
					if (iter.hasNext())
					{
						expression = iter.next();
						iter.remove();
					}
					else
					{
						fail("-e option requires an expression");
					}
					break;

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
						fail("-fail requires a log file name");
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
	    			
	    		case "-remote":
	    			iter.remove();
	    			startInterpreter = true;
	    			interactive = false;
	    			
	    			if (iter.hasNext())
	    			{
	    				remoteControlName = iter.next();
	    				iter.remove();
	    			}
	    			else
	    			{
	    				fail("-remote option requires a Java classname");
	    			}
	    			break;
	    			
	    		case "-simulation":
	    			iter.remove();
	    			startInterpreter = true;
	    			interactive = true;
	    			
	    			if (iter.hasNext())
	    			{
	    				remoteSimulationName = iter.next();
	    				iter.remove();
	    			}
	    			else
	    			{
	    				fail("-simulation option requires a Java classname");
	    			}
	    			break;
			}
		}
		
		if (logfile != null && Settings.dialect != Dialect.VDM_RT)
		{
			fail("The -log option can only be used with -vdmrt");
		}
		
		if (remoteControlName != null && remoteSimulationName != null)
		{
			fail("The -remote and -simulation options cannot be used together");
		}
		
		if (remoteSimulationName != null && Settings.dialect != Dialect.VDM_RT)
		{
			fail("The -simulation option can only be used with -vdmrt");
		}

		if (remoteControlName != null)
		{
			remoteClass = getRemoteClass(remoteControlName);
		}

		if (remoteSimulationName != null)
		{
			remoteSimulation = getRemoteClass(remoteSimulationName);
			
			try
			{
				remoteSimulation.getDeclaredConstructor().newInstance();
			}
			catch (Exception e)
			{
				fail("Cannot instantiate simulation: " + e.getMessage());
			}
		}
	}
	
	@Override
	public List<VDMMessage> handleEvent(Event event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			return interpreterPrepare();
		}
		else if (event instanceof CheckCompleteEvent)
		{
			return startInterpreter ? interpreterInit() : null;
		}
		else
		{
			throw new Exception("Unhandled event: " + event);
		}
	}
	
	abstract public List<VDMMessage> interpreterPrepare();

	abstract public List<VDMMessage> interpreterInit();

	abstract public ExitStatus interpreterRun();

	abstract public <T extends Mappable> T getIN();

	@SuppressWarnings("unchecked")
	private static <T> T getRemoteClass(String remoteName)
	{
		try
		{
			return (T) ClassLoader.getSystemClassLoader().loadClass(remoteName);
		}
		catch (ClassNotFoundException e)
		{
			fail("Cannot locate " + remoteName + " on the CLASSPATH");
		}
		catch (ClassCastException e)
		{
			fail(remoteName + " does not implement interface");
		}
		
		return null;
	}

	public boolean runNeeded()
	{
		return startInterpreter;	// Because we need it for something!
	}
	
	public boolean isInteractive()
	{
		return interactive;
	}
	
	@Override
	public AnalysisCommand getCommand(String[] argv)
	{
		switch (argv[0])
		{
			case "print":
			case "p":
				return new PrintCommand(argv);
				
			default:
				return null;
		}
	}
	
	@Override
	public void help()
	{
		println("[p]rint <exp>: evaluate expression");
	}
}
