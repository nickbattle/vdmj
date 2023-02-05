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

import static com.fujitsu.vdmj.plugins.PluginConsole.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
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
import com.fujitsu.vdmj.plugins.events.CheckCompleteEvent;
import com.fujitsu.vdmj.plugins.events.CheckFailedEvent;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.plugins.events.CheckSyntaxEvent;
import com.fujitsu.vdmj.plugins.events.CheckTypeEvent;
import com.fujitsu.vdmj.plugins.events.Event;
import com.fujitsu.vdmj.plugins.events.ShutdownEvent;
import com.fujitsu.vdmj.util.GetResource;

/**
 * The main class for the plugin based VDMJ.
 */
public class VDMJ
{
	private static List<String> argv = null;
	private static List<File> paths = null;
	private static boolean nowarn = false;

	public static void main(String[] args)
	{
		argv = new Vector<String>(Arrays.asList(args));
		paths = new Vector<File>();
		nowarn = false;
		
		Properties.init();
		setDialect(argv);
		loadPlugins();
		processArgs();
		
		ExitStatus result = ExitStatus.EXIT_OK;
		
		do
		{
			findFiles();
			
			if (checkAndInitFiles())
			{
				if (runNeeded())
				{
					result = run();
				}
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
	
	private static void setDialect(List<String> argv2)
	{
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
			fail("You must set either -vdmsl, -vdmpp or -vdmrt");
		}
	}

	private static void usage()
	{
		Map<String, AnalysisPlugin> plugins = PluginRegistry.getInstance().getPlugins();
		
		println("Usage: VDMJ <-vdmsl | -vdmpp | -vdmrt> [<options>] [<files or dirs>]");
		println("-vdmsl: parse files as VDM-SL");
		println("-vdmpp: parse files as VDM++");
		println("-vdmrt: parse files as VDM-RT");
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
					nowarn = true;
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
	    			String version = getVersion();
	
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

				case "-verbose":
					Settings.verbose = true;
					iter.remove();
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
	
	private static void loadPlugins()
	{
		try
		{
			PluginRegistry registry = PluginRegistry.getInstance();
			verbose("Registering standard plugins");

			ASTPlugin ast = ASTPlugin.factory(Settings.dialect);
			registry.registerPlugin(ast);

			TCPlugin tc = TCPlugin.factory(Settings.dialect);
			registry.registerPlugin(tc);

			INPlugin in = INPlugin.factory(Settings.dialect);
			registry.registerPlugin(in);
			
			POPlugin po = POPlugin.factory(Settings.dialect);
			registry.registerPlugin(po);
			
			if (System.getProperty("vdmj.plugins") != null)
			{
				String[] plugins = System.getProperty("vdmj.plugins").split("\\s*[,;]\\s*");
				
				for (String plugin: plugins)
				{
					try
					{
						Class<?> clazz = Class.forName(plugin);
						Method factory = clazz.getMethod("factory", Dialect.class);
						AnalysisPlugin instance = (AnalysisPlugin)factory.invoke(null, Settings.dialect);
						registry.registerPlugin(instance);
						verbose("Registered " + plugin);
					}
					catch (Exception e)
					{
						println("Cannot load plugin: " + plugin);
						throw e;
					}
				}
			}
		}
		catch (Exception e)
		{
			println(e);
			System.exit(1);
		}
	}
	
	private static void findFiles()
	{
		List<File> filenames = new Vector<File>();
		
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
						filenames.add(subfile);
					}
				}
			}
			else if (file.exists() || BacktrackInputReader.isExternalFormat(file))
			{
				filenames.add(file);
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
							filenames.add(pfile);
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
							filenames.add(GetResource.load(file, dest));
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
		
		ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
		INPlugin in = PluginRegistry.getInstance().getPlugin("IN");
		ast.setFiles(filenames);
		
		if (filenames.isEmpty() && !in.isInteractive())
		{
			fail("You did not identify any source files");
		}
	}
	
	private static boolean checkAndInitFiles()
	{
		try
		{
			EventHub eventhub = EventHub.getInstance();
			Event event = new CheckPrepareEvent();
			List<VDMMessage> messages = eventhub.publish(event);

			if (report(messages, "Prepared", "preparation"))
			{
				event = new CheckSyntaxEvent();
				messages = eventhub.publish(event);
				
				if (report(messages, "Parsed", "syntax"))
				{
					event = new CheckTypeEvent();
					messages = eventhub.publish(event);

					if (report(messages, "Type checked", "type"))
					{
						event = new CheckCompleteEvent();
						messages = eventhub.publish(event);

						if (report(messages, "Initialized", "init"))
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
	
	private static boolean report(List<VDMMessage> messages, String verb, String kind)
	{
		int errors = 0;
		int warnings = 0;
		
		for (VDMMessage m: messages)
		{
			if (m instanceof VDMError)
			{
				println(m.toString());
				errors++;
			}
			else if (m instanceof VDMWarning)
			{
				if (!nowarn) println(m.toString());
				warnings++;
			}
		}
		
		ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
		int count = ast.getCount();

		if (count > 0)	// Just using -i gives count = 0
		{
			String objects = Settings.dialect == Dialect.VDM_SL ?
				plural(count, "module", "s") :
				plural(count, "class", "es");
				
			double duration = (double)(EventHub.getInstance().getLastDuration())/1000;
			
			if (errors > 0 || warnings > 0 || !verb.equals("Prepared"))
			{
		   		info(verb + " " + objects + " in " + duration + " secs. ");
		   		info(errors == 0 ? "No " + kind + " errors" : "Found " + plural(errors, kind + " error", "s"));
		  		infoln(warnings == 0 ? "" : " and " + (nowarn ? "suppressed " : "") + plural(warnings, "warning", "s"));
			}
		}
		
		return (errors == 0);	// Return "OK" if we can continue (ie. no errors)
	}
	
	private static boolean runNeeded()
	{
		INPlugin in = PluginRegistry.getInstance().getPlugin("IN");
		return in.runNeeded();
	}

	private static ExitStatus run()
	{
		INPlugin in = PluginRegistry.getInstance().getPlugin("IN");
		return in.interpreterRun();
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
	
	private static String getVersion()
	{
		try
		{
			String path = VDMJ.class.getName().replaceAll("\\.", "/");
			URL url = VDMJ.class.getResource("/" + path + ".class");
			JarURLConnection conn = (JarURLConnection)url.openConnection();
		    JarFile jar = conn.getJarFile();
			Manifest mf = jar.getManifest();
			String version = (String)mf.getMainAttributes().get(Attributes.Name.IMPLEMENTATION_VERSION);
			return version;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
