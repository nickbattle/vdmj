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

package com.fujitsu.vdmj.plugins;

import static com.fujitsu.vdmj.plugins.PluginConsole.fail;
import static com.fujitsu.vdmj.plugins.PluginConsole.info;
import static com.fujitsu.vdmj.plugins.PluginConsole.infoln;
import static com.fujitsu.vdmj.plugins.PluginConsole.plural;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static com.fujitsu.vdmj.plugins.PluginConsole.validateCharset;
import static com.fujitsu.vdmj.plugins.PluginConsole.verbose;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.VDMJMain;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.BacktrackInputReader;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.plugins.analyses.ASTPlugin;
import com.fujitsu.vdmj.plugins.analyses.INPlugin;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.plugins.analyses.TCPlugin;
import com.fujitsu.vdmj.plugins.events.AbstractCheckFilesEvent;
import com.fujitsu.vdmj.plugins.events.CheckCompleteEvent;
import com.fujitsu.vdmj.plugins.events.CheckFailedEvent;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.plugins.events.CheckSyntaxEvent;
import com.fujitsu.vdmj.plugins.events.CheckTypeEvent;
import com.fujitsu.vdmj.plugins.events.ShutdownEvent;
import com.fujitsu.vdmj.plugins.events.StartConsoleEvent;
import com.fujitsu.vdmj.util.GetResource;
import com.fujitsu.vdmj.util.Utils;

/**
 * The main class for the plugin based VDMJ.
 */
public class VDMJ implements VDMJMain
{
	private static List<String> argv = null;
	private static List<File> paths = null;
	private static List<File> files = null;
	private static boolean warnings = true;
	
	public static String getMainName()
	{
		return VDMJ_MAIN;
	}

	public static void main(String[] args)
	{
		Settings.mainClass = VDMJ.class;
		argv = new Vector<String>(Arrays.asList(args));
		paths = new Vector<File>();
		files = new Vector<File>();
		warnings = true;
		
		Properties.init();
		setDialect();
		loadPlugins();
		processArgs();
		
		ExitStatus result = ExitStatus.EXIT_OK;
		
		do
		{
			findFiles();
			
			if (checkAndInitFiles())
			{
				result = run();
			}
			else
			{
				result = ExitStatus.EXIT_ERRORS;
			}

			complete();
		}
		while (result == ExitStatus.RELOAD);
		
		infoln("Bye");
		System.exit(result == ExitStatus.EXIT_OK ? 0 : 1);
	}
	
	private static void setDialect()
	{
		if (argv.contains("-verbose"))
		{
			argv.remove("-verbose");
			Settings.verbose = true;
		}

		if (argv.contains("-vdmsl"))
		{
			argv.remove("-vdmsl");
			Settings.dialect = Dialect.VDM_SL;
		}
		else if (argv.contains("-vdmpp"))
		{
			argv.remove("-vdmpp");
			Settings.dialect = Dialect.VDM_PP;
		}
		else if (argv.contains("-vdmrt"))
		{
			argv.remove("-vdmrt");
			Settings.dialect = Dialect.VDM_RT;
		}
		else
		{
			verbose("Setting dialect to VDM-SL by default");
			Settings.dialect = Dialect.VDM_SL;
		}
	}

	/**
	 * These setters are used by DBGPReader, so that it can use the other
	 * VDMJ methods to parse/check the spec.
	 */
	public static void setFiles(List<File> files)
	{
		VDMJ.files = files;
	}

	public static void setArgs(String... args)
	{
		argv = new Vector<String>(Arrays.asList(args));
		processArgs();
	}

	private static void usage()
	{
		Map<String, AnalysisPlugin> plugins = PluginRegistry.getInstance().getPlugins();
		
		println("Usage: VDMJ [-vdmsl | -vdmpp | -vdmrt] [<options>] [<files or dirs>]");
		println("-vdmsl: parse files as VDM-SL (default)");
		println("-vdmpp: parse files as VDM++");
		println("-vdmrt: parse files as VDM-RT");
		println("-w: suppress warning messages");
		println("-v: show VDMJ jar version");
		println("-path: search path for files");
		println("-strict: use strict grammar rules");
		println("-r <release>: VDM language release");
		println("-t <charset>: select a console charset");
		println("-q: suppress information messages");
		println("-verbose: display detailed startup information");
		
		for (AnalysisPlugin plugin: plugins.values())
		{
			plugin.usage();
		}
		
		System.exit(0);
	}
	
	private static void processArgs()
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			switch (iter.next())
			{
				case "-w":
					iter.remove();
					warnings = false;
					break;
					
				case "-r":
	    			iter.remove();
	    			
	    			if (iter.hasNext())
	    			{
	    				Settings.release = Release.lookup(iter.next());
	       				iter.remove();
	       			 
	    				if (Settings.release == null)
	    				{
	    					fail("-r option must be " + Release.list());
	    				}
	    			}
	    			else
	    			{
	    				fail("-r option requires a VDM release");
	    			}
	    			break;

				case "-v":		// Exit if this option is used.
	    			iter.remove();
	    			String version = Utils.getVersion();
	
	    			if (version == null)
	    			{
	    				println("Cannot determine VDMJ version");
	        			System.exit(1);
	    			}
	    			else
	    			{
	    				println("VDMJ version = " + version);
	        			System.exit(0);
	    			}
	    			break;

	    		case "-t":
	    			iter.remove();
	    			
	    			if (iter.hasNext())
	    			{
	    				Console.init(validateCharset(iter.next()));
	    				iter.remove();
	    			}
	    			else
	    			{
	    				fail("-t option requires a charset name");
	    			}
	    			break;

				case "-path":
					iter.remove();
					
					if (iter.hasNext())
					{
						paths.add(new File(iter.next()));
						iter.remove();
					}
					else
					{
						fail("-path requires a directory");
					}
	    			break;

				case "-annotations":
					Settings.annotations = true;
					iter.remove();
	    			break;

				case "-strict":
					Settings.strict = true;
					iter.remove();
	    			break;

				case "-q":
					PluginConsole.setQuiet(true);
					iter.remove();
	    			break;

				case "-help":
				case "-?":
					iter.remove();
					usage();
	    			break;
			}
		}
		
		Map<String, AnalysisPlugin> plugins = PluginRegistry.getInstance().getPlugins();
		
		for (AnalysisPlugin plugin: plugins.values())
		{
			plugin.processArgs(argv);
		}
	}
	
	public static void loadPlugins()
	{
		try
		{
			PluginRegistry registry = PluginRegistry.getInstance();
			verbose("Registering standard plugins");

			ASTPlugin ast = ASTPlugin.factory(Settings.dialect);
			registry.registerPlugin(ast);
			verbose("Registered AST plugin");

			TCPlugin tc = TCPlugin.factory(Settings.dialect);
			registry.registerPlugin(tc);
			verbose("Registered TC plugin");

			INPlugin in = INPlugin.factory(Settings.dialect);
			registry.registerPlugin(in);
			verbose("Registered IN plugin");
			
			POPlugin po = POPlugin.factory(Settings.dialect);
			registry.registerPlugin(po);
			verbose("Registered PO plugin");
			
			List<String> userPlugins = GetResource.readResource("vdmj.plugins");
			
			if (!userPlugins.isEmpty())
			{
				for (String plugin: userPlugins)
				{
					try
					{
						Class<?> clazz = Class.forName(plugin);
						Method factory = clazz.getMethod("factory", Dialect.class);
						AnalysisPlugin instance = (AnalysisPlugin)factory.invoke(null, Settings.dialect);
						registry.registerPlugin(instance);
						verbose("Registered " + plugin + " plugin");
					}
					catch (InvocationTargetException e)
					{
						println("vdmj.plugins = " + System.getProperty("vdmj.plugins"));
						println("Cannot load plugin: " + plugin);
						throw e.getCause();
					}
					catch (NoSuchMethodException e)
					{
						println("vdmj.plugins = " + System.getProperty("vdmj.plugins"));
						println("Cannot load plugin: " + plugin);
						throw e;
					}
				}
			}
		}
		catch (Throwable e)
		{
			println(e);
			throw new RuntimeException("Cannot load plugins");
		}
	}
	
	private static void findFiles()
	{
		files = new Vector<File>();
		
		for (String arg: argv)
		{
			if (arg.startsWith("-"))
			{
				// All legal options and their arguments should have been removed by
				// this point, so we assume filenames cannot start with "-"!
				
				println("Unexpected option: " + arg);
				usage();
			}
			
			File file = new File(arg);
	
			if (file.isDirectory())
			{
				for (File subfile: file.listFiles(Settings.dialect.getFilter()))
				{
					if (subfile.isFile())
					{
						files.add(subfile);
					}
				}
			}
			else if (file.exists() || BacktrackInputReader.isExternalFormat(file))
			{
				files.add(file);
			}
			else
			{
				boolean found = false;
				
				if (!file.isAbsolute())
				{
					for (File path: paths)
					{
						File pfile = new File(path.getPath(), file.getPath());
						
						if (pfile.exists())
						{
							files.add(pfile);
							found = true;
							break;
						}
					}
				}
				
				if (!found)
				{
					if (GetResource.find(file))
					{
						try
						{
							File lib = new File("lib");
							lib.mkdir();
							File dest = new File(lib, file.getName());
							files.add(GetResource.load(file, dest));
						}
						catch (IOException e)
						{
							fail("Cannot load resource /" + file.getName() + ": " + e.getMessage());
						}
	    			}
					else
					{
						fail("Cannot find file " + file);
					}
				}
			}
		}
		
		verbose("Found %d files", files.size());
	}
	
	public static boolean checkAndInitFiles()
	{
		try
		{
			EventHub eventhub = EventHub.getInstance();
			AbstractCheckFilesEvent event = new CheckPrepareEvent(files);
			List<VDMMessage> messages = eventhub.publish(event);

			if (report(messages, event))
			{
				event = new CheckSyntaxEvent();
				messages = eventhub.publish(event);
				
				if (report(messages, event))
				{
					event = new CheckTypeEvent();
					messages = eventhub.publish(event);

					if (report(messages, event))
					{
						event = new CheckCompleteEvent();
						messages = eventhub.publish(event);

						if (report(messages, event))
						{
							verbose("Loaded files initialized successfully");
							return true;
						}
						else
						{
							verbose("Failed to initialize interpreter");
							messages.addAll(eventhub.publish(new CheckFailedEvent(event)));
						}
					}
					else
					{
						verbose("Type checking errors found");
						messages.addAll(eventhub.publish(new CheckFailedEvent(event)));
					}
				}
				else
				{
					verbose("Syntax errors found");
					messages.addAll(eventhub.publish(new CheckFailedEvent(event)));
				}
			}
			else
			{
				verbose("Preparation errors found");
				messages.addAll(eventhub.publish(new CheckFailedEvent(event)));
			}
		}
		catch (Exception e)
		{
			println(e);
			System.exit(1);
		}
		
		return false;
	}
	
	private static int count(List<VDMMessage> messages, Class<? extends VDMMessage>type)
	{
		int count = 0;

		for (VDMMessage m: messages)
		{
			if (type.isAssignableFrom(m.getClass()))
			{
				println(m.toString());
				count++;
			}
		}
		
		return count;
	}
	
	private static boolean report(List<VDMMessage> messages, AbstractCheckFilesEvent event)
	{
		int nerrs  = count(messages, VDMError.class);
		int nwarns = count(messages, VDMWarning.class);
		
		ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
		int count = ast.getCount();

		if (count > 0)	// Just using -i gives count = 0
		{
			String objects = Settings.dialect == Dialect.VDM_SL ?
				plural(count, "module", "s") :
				plural(count, "class", "es");
				
			double duration = (double)(EventHub.getInstance().getLastDuration())/1000;
			String title = event.getProperty(AbstractCheckFilesEvent.TITLE);
			String kind = event.getProperty(AbstractCheckFilesEvent.KIND);
			
			if (nerrs > 0 || nwarns > 0 || !title.equals("Prepared"))
			{
		   		info(title + " " + objects + " in " + duration + " secs. ");
		   		info(nerrs == 0 ? "No " + kind + " errors" : "Found " + plural(nerrs, kind + " error", "s"));
		  		infoln(nwarns == 0 ? "" : " and " + (warnings ? "" : "suppressed ") + plural(nwarns, "warning", "s"));
			}
		}
		
		return (nerrs == 0);	// Return "OK" if we can continue (ie. no errors)
	}
	
	private static ExitStatus run()
	{
		try
		{
			StartConsoleEvent event = new StartConsoleEvent();
			EventHub.getInstance().publish(event);
			return event.getStatus();
		}
		catch (Exception e)
		{
			println(e);
			return ExitStatus.EXIT_ERRORS;
		}
	}

	private static void complete()
	{
		try
		{
			EventHub.getInstance().publish(new ShutdownEvent());
		}
		catch (Exception e)
		{
			println(e);
		}
	}
}
