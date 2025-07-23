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

import static com.fujitsu.vdmj.plugins.PluginConsole.fail;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import java.util.Iterator;
import java.util.List;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.RemoteControl;
import com.fujitsu.vdmj.RemoteSimulation;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.AnalysisEvent;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.HelpList;
import com.fujitsu.vdmj.plugins.commands.AssertCommand;
import com.fujitsu.vdmj.plugins.commands.ClassesCommand;
import com.fujitsu.vdmj.plugins.commands.CoverageCommand;
import com.fujitsu.vdmj.plugins.commands.CreateCommand;
import com.fujitsu.vdmj.plugins.commands.DebugCommand;
import com.fujitsu.vdmj.plugins.commands.DefaultCommand;
import com.fujitsu.vdmj.plugins.commands.EnvCommand;
import com.fujitsu.vdmj.plugins.commands.FilesCommand;
import com.fujitsu.vdmj.plugins.commands.InitCommand;
import com.fujitsu.vdmj.plugins.commands.LatexCommand;
import com.fujitsu.vdmj.plugins.commands.LogCommand;
import com.fujitsu.vdmj.plugins.commands.ModulesCommand;
import com.fujitsu.vdmj.plugins.commands.PluginsCommand;
import com.fujitsu.vdmj.plugins.commands.PrintCommand;
import com.fujitsu.vdmj.plugins.commands.RuntraceCommand;
import com.fujitsu.vdmj.plugins.commands.SaveCommand;
import com.fujitsu.vdmj.plugins.commands.ScriptCommand;
import com.fujitsu.vdmj.plugins.commands.SetCommand;
import com.fujitsu.vdmj.plugins.commands.StateCommand;
import com.fujitsu.vdmj.plugins.commands.ThreadsCommand;
import com.fujitsu.vdmj.plugins.commands.WordCommand;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.plugins.events.StartConsoleEvent;

/**
 * CMD command line plugin
 */
abstract public class CMDPlugin extends AnalysisPlugin implements EventListener
{
	protected boolean startInterpreter;		// eg. -e, -cmd or -remote as well as -i
	protected boolean interactive;			// eg. -i or -simulation, not -e or -cmd
	protected String expression;			// eg. -e "f(1, 2)"
	protected String commandline;			// eg. -cmd "qc -s fixed"
	protected RemoteControl remoteInstance;
	
	@Override
	public String getName()
	{
		return "CMD";
	}
	
	@Override
	public int getPriority()
	{
		return CMD_PRIORITY;
	}

	@Override
	public void init()
	{
		startInterpreter = false;
		interactive = false;
		expression = null;
		commandline = null;
		remoteInstance = null;
		
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(StartConsoleEvent.class, this);
	}

	public static CMDPlugin factory(Dialect dialect) throws Exception
	{
		switch (dialect)
		{
			case VDM_SL:
				return new CMDPluginSL();
				
			case VDM_PP:
				return new CMDPluginPP();
				
			case VDM_RT:
				return new CMDPluginRT();
				
			default:
				throw new Exception("Unknown dialect: " + dialect);
		}
	}
	
	@Override
	public void usage()
	{
		println("-i: run the interpreter if successfully type checked");
		println("-e <exp>: evaluate <exp> and stop");
		println("-cmd <command>: perform <command> and stop");
		println("-remote <class>: enable remote control");
		println("-simulation <class>: enable simulation control");
	}
	
	@Override
	public void processArgs(List<String> argv)
	{
		String remoteControlName = null;
		String remoteSimulationName = null;

		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			switch(iter.next())
			{			
				case "-i":
					iter.remove();
					setStartInterpreter();
					interactive = true;
					break;
					
				case "-e":
					iter.remove();
					setStartInterpreter();
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

				case "-cmd":
					iter.remove();
					setStartInterpreter();
					interactive = false;
					
					if (iter.hasNext())
					{
						commandline = iter.next();
						iter.remove();
					}
					else
					{
						fail("-cmd option requires a command");
					}
					break;

	    		case "-remote":
	    			iter.remove();
					setStartInterpreter();
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
					setStartInterpreter();
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
		
		if (remoteControlName != null)
		{
			try
			{
				Class<RemoteControl> remoteClass = findClass(remoteControlName);
				remoteInstance = remoteClass.getDeclaredConstructor().newInstance();
			}
			catch (Exception e)
			{
				fail("Cannot instantiate remote: " + e.getMessage());
			}
		}
		else if (remoteSimulationName != null)
		{
			if (Settings.dialect != Dialect.VDM_RT)
			{
				fail("The -simulation option can only be used with -vdmrt");
			}

			try
			{
				Class<RemoteSimulation> remoteSimulation = findClass(remoteSimulationName);
				remoteSimulation.getDeclaredConstructor().newInstance();
				// discovered by RemoteSimulation.getInstance()
			}
			catch (Exception e)
			{
				fail("Cannot instantiate simulation: " + e.getMessage());
			}
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
	
	public boolean startInterpreter()
	{
		return startInterpreter;
	}
	
	public boolean isInteractive()
	{
		return interactive;
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

			return null;
		}
		else if (event instanceof StartConsoleEvent)
		{
			if (startInterpreter)
			{
				StartConsoleEvent sevent = (StartConsoleEvent)event;
				sevent.setStatus(interpreterRun());
			}

			return null;	// No errors needed?
		}
		else
		{
			throw new Exception("Unhandled event: " + event);
		}
	}

	abstract protected ExitStatus interpreterRun();

	@SuppressWarnings("unchecked")
	private static <T> T findClass(String classname)
	{
		try
		{
			return (T) ClassLoader.getSystemClassLoader().loadClass(classname);
		}
		catch (ClassNotFoundException e)
		{
			fail("Cannot locate " + classname + " on the CLASSPATH");
		}
		catch (ClassCastException e)
		{
			fail(classname + " does not implement interface");
		}
		
		return null;
	}

	@Override
	public AnalysisCommand getCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		switch (parts[0])
		{
			case "init":		return new InitCommand(line);
			case "set":			return new SetCommand(line);
			case "default":		return new DefaultCommand(line);
			case "modules":		return new ModulesCommand(line);
			case "classes":		return new ClassesCommand(line);
			case "files":		return new FilesCommand(line);
			case "plugins":		return new PluginsCommand(line);
			case "env":			return new EnvCommand(line);
			case "state":		return new StateCommand(line);
			case "log":
			case "validate":	return new LogCommand(line);
			case "print":
			case "p":			return new PrintCommand(line);
			case "script":		return new ScriptCommand(line);
			case "assert":		return new AssertCommand(line);
			case "threads":		return new ThreadsCommand(line);
			case "create":		return new CreateCommand(line);
			case "break":
			case "trace":
			case "catch":
			case "list":
			case "remove":		return new DebugCommand(line);
			case "coverage":	return new CoverageCommand(line);
			case "latex":		return new LatexCommand(line);
			case "word":		return new WordCommand(line);
			case "save":		return new SaveCommand(line);

			default:
				return null;
		}
	}
	
	@Override
	public HelpList getCommandHelp()
	{
		HelpList list = new HelpList(
			InitCommand.HELP,
			SetCommand.HELP,
			DefaultCommand.HELP,
			FilesCommand.HELP,
			PluginsCommand.HELP,
			EnvCommand.HELP,
			PrintCommand.HELP,
			ScriptCommand.HELP,
			AssertCommand.HELP,
			CoverageCommand.HELP,
			LatexCommand.HELP,
			WordCommand.HELP,
			SaveCommand.HELP
		);
		
		list.add(DebugCommand.help());
		
		if (Settings.dialect == Dialect.VDM_SL)
		{
			list.add(ModulesCommand.HELP);
			list.add(StateCommand.HELP);
		}
		else
		{
			list.add(ClassesCommand.HELP);
			list.add(CreateCommand.HELP);
			list.add(LogCommand.help());
			list.add(ThreadsCommand.HELP);
		}
		
		return list;
	}
}
