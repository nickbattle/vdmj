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
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static com.fujitsu.vdmj.plugins.PluginConsole.validateCharset;
import static com.fujitsu.vdmj.plugins.PluginConsole.verbose;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.BacktrackInputReader;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.plugins.analyses.ASTPlugin;
import com.fujitsu.vdmj.plugins.analyses.INPlugin;
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

	public static void main(String[] args)
	{
		argv = new Vector<String>(Arrays.asList(args));
		paths = new Vector<File>();
		
		Properties.init();

		processArgs();
		loadPlugins();
		findFiles();
		complete(checkAndInitFiles());
	}
	
	private static void processArgs()
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			String arg = iter.next();
			
			if (arg.equals("-vdmsl"))
			{
				iter.remove();
				Settings.dialect = Dialect.VDM_SL;
			}
			else if (arg.equals("-vdmpp"))
			{
				iter.remove();
				Settings.dialect = Dialect.VDM_PP;
			}
			else if (arg.equals("-vdmrt"))
			{
				iter.remove();
				Settings.dialect = Dialect.VDM_RT;
			}
    		else if (arg.equals("-r"))
    		{
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
    		}
    		else if (arg.equals("-v"))		// Exit if this option is used.
    		{
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
    		}
    		else if (arg.equals("-t"))
    		{
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
    		}
			else if (arg.equals("-path"))
			{
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
			}
			else if (arg.equals("-verbose"))
			{
				Settings.verbose = true;
				iter.remove();
			}
			else if (arg.equals("-annotations"))
			{
				Settings.annotations = true;
				iter.remove();
			}
			else if (arg.equals("-strict"))
			{
				Settings.strict = true;
				iter.remove();
			}
			else if (arg.equals("-q"))
			{
				PluginConsole.quiet = true;
				iter.remove();
			}
		}
		
		if (Settings.dialect == null)
		{
			fail("You must set -vdmsl, -vdmpp or -vdmrt");
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
			ast.processArgs(argv);

			TCPlugin tc = TCPlugin.factory(Settings.dialect);
			registry.registerPlugin(tc);
			tc.processArgs(argv);

			INPlugin in = INPlugin.factory(Settings.dialect);
			registry.registerPlugin(in);
			in.processArgs(argv);
			
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
				
				fail("Unexpected option: " + arg);
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
		ast.setFiles(filenames);
	}
	
	private static boolean checkAndInitFiles()
	{
		try
		{
			EventHub eventhub = EventHub.getInstance();
			List<VDMError> errList = new Vector<VDMError>();
			
			Event event = new CheckPrepareEvent();
			errList = eventhub.publish(event);

			if (errList.isEmpty())
			{
				event = new CheckSyntaxEvent();
				errList.addAll(eventhub.publish(event));
				
				if (errList.isEmpty())
				{
					event = new CheckTypeEvent();
					errList.addAll(eventhub.publish(event));

					if (errList.isEmpty())
					{
						event = new CheckCompleteEvent();
						errList.addAll(eventhub.publish(event));

						if (errList.isEmpty())
						{
							verbose("Loaded files initialized successfully");
							return true;
						}
						else
						{
							verbose("Failed to initialize interpreter");
							errList.addAll(eventhub.publish(new CheckFailedEvent(event)));
						}
					}
					else
					{
						verbose("Type checking errors found");
						errList.addAll(eventhub.publish(new CheckFailedEvent(event)));
					}
				}
				else
				{
					verbose("Syntax errors found");
					errList.addAll(eventhub.publish(new CheckFailedEvent(event)));
				}
			}
			else
			{
				verbose("Preparation errors found");
				errList.addAll(eventhub.publish(new CheckFailedEvent(event)));
			}
		}
		catch (Exception e)
		{
			println(e);
			System.exit(1);
		}
		
		return false;
	}

	private static void complete(boolean success)
	{
		try
		{
			EventHub.getInstance().publish(new ShutdownEvent());
			System.exit(success ? 0 : 1);
		}
		catch (Exception e)
		{
			println(e);
			System.exit(1);
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
