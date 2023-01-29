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
import static com.fujitsu.vdmj.plugins.PluginConsole.verbose;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.BacktrackInputReader;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.plugins.analyses.ASTPlugin;
import com.fujitsu.vdmj.plugins.analyses.TCPlugin;
import com.fujitsu.vdmj.plugins.events.CheckCompleteEvent;
import com.fujitsu.vdmj.plugins.events.CheckFailedEvent;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.plugins.events.CheckSyntaxEvent;
import com.fujitsu.vdmj.plugins.events.CheckTypeEvent;
import com.fujitsu.vdmj.plugins.events.Event;
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

		if (argv.contains("-vdmsl"))
		{
			Settings.dialect = Dialect.VDM_SL;
			argv.remove("-vdmsl");
			
			if (argv.contains("-vdmpp") || argv.contains("-vdmrt"))
			{
				fail("Must include one of -vdmsl, -vdmpp or -vdmrt");
			}
		}
		else if (argv.contains("-vdmpp"))
		{
			Settings.dialect = Dialect.VDM_PP;
			argv.remove("-vdmpp");
			
			if (argv.contains("-vdmsl") || argv.contains("-vdmrt"))
			{
				fail("Must include one of -vdmsl, -vdmpp or -vdmrt");
			}
		}
		else if (argv.contains("-vdmrt"))
		{
			Settings.dialect = Dialect.VDM_RT;
			argv.remove("-vdmrt");
			
			if (argv.contains("-vdmpp") || argv.contains("-vdmsl"))
			{
				fail("Must include one of -vdmsl, -vdmpp or -vdmrt");
			}
		}
		else
		{
			fail("Must include one of -vdmsl, -vdmpp or -vdmrt");
		}
		
		verbose("Dialect set to " + Settings.dialect);
		
		processArgs();
		loadPlugins();
		findFiles();
		checkFiles();
	}
	
	private static void processArgs()
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			String arg = iter.next();
			
			if (arg.equals("-path"))
			{
				iter.remove();
				
				if (iter.hasNext())
				{
					paths.add(new File(iter.next()));
					iter.remove();
				}
			}
			else if (arg.equals("-verbose"))
			{
				Settings.verbose = true;
				iter.remove();
			}
			else if (arg.equals("-q"))
			{
				PluginConsole.quiet = true;
				iter.remove();
			}
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
		
		if (filenames.isEmpty())
		{
			fail("You have not specified any source files");
		}
		else
		{
			ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
			ast.setFiles(filenames);
		}
	}
	
	private static void checkFiles()
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
							verbose("Loaded files checked successfully");
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
	}
}
