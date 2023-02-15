/*******************************************************************************
 *
 *	Copyright (c) 2013 Fujitsu Services Ltd.
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

package com.fujitsu.vdmjunit;

import static com.fujitsu.vdmj.plugins.PluginConsole.info;
import static com.fujitsu.vdmj.plugins.PluginConsole.infoln;
import static com.fujitsu.vdmj.plugins.PluginConsole.plural;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static com.fujitsu.vdmj.plugins.PluginConsole.verbose;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.plugins.EventHub;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.VDMJ;
import com.fujitsu.vdmj.plugins.analyses.ASTPlugin;
import com.fujitsu.vdmj.plugins.events.AbstractCheckFilesEvent;
import com.fujitsu.vdmj.plugins.events.CheckCompleteEvent;
import com.fujitsu.vdmj.plugins.events.CheckFailedEvent;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.plugins.events.CheckSyntaxEvent;
import com.fujitsu.vdmj.plugins.events.CheckTypeEvent;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.util.GetResource;

/**
 * The abstract parent class of all specification readers.
 */
abstract public class SpecificationReader
{
	protected List<VDMError> errors = new Vector<VDMError>();
	protected List<VDMWarning> warnings = new Vector<VDMWarning>();

	/**
	 * Construct a SpecificationReader for a particular VDM dialect.
	 * 
	 * @param dialect
	 */
	public SpecificationReader(Dialect dialect)
	{
		Properties.init();
		Settings.dialect = dialect;
		PluginRegistry.reset();
		EventHub.reset();
		VDMJ.loadPlugins();
		VDMJ.setArgs("-i");		// Because we want an interpreter
	}
	
	/**
	 * Print a list of error messages to stdout. This is used internally by the
	 * readSpecification method to report syntax or type checking errors before
	 * stopping.
	 * 
	 * @param messages A list of messages to print.
	 */
	protected void printMessages(List<? extends VDMMessage> messages)
	{
		for (VDMMessage message: messages)
		{
			System.out.println(message);
		}
	}
	
	/**
	 * Parse and type check source files, and create an interpreter. Directories are
	 * expanded to include any VDM source files they contain.
	 * 
	 * @param charset The charset for the specification files.
	 * @param filenames A list of VDM source files or directories.
	 * @throws Exception 
	 */
	public Interpreter readSpecification(Charset charset, String... filenames) throws Exception
	{
		Settings.filecharset = charset;
		List<File> list = new Vector<File>(filenames.length);
		
		for (String filename: filenames)
		{
			URL url = getClass().getResource("/" + filename);
			
			if (url == null)
			{
				throw new FileNotFoundException(filename);
			}
			
			File file = null;
			
			if (url.getProtocol().equals("jar"))
			{
				file = GetResource.load(new File("/" + filename));
			}
			else
			{
				file = new File(url.toURI());
			}
			
			if (file.isDirectory())
			{
				for (File subfile: file.listFiles(Settings.dialect.getFilter()))
				{
					if (subfile.isFile())
					{
						list.add(subfile);
					}
				}
			}
			else
			{
				list.add(file);
			}
		}
		
		if (!pluginLifecycle(list))
		{
			fail("Type errors (see stdout)");
		}

		return Interpreter.getInstance();
	}

	/**
	 * Return the syntax and type checking errors from the last readSpecification
	 * @return a list of errors
	 */
	public List<VDMError> getErrors()
	{
		return errors;
	}

	/**
	 * Return the warnings from the last readSpecification
	 * @return a list of warnings
	 */
	public List<VDMWarning> getWarnings()
	{
		return warnings;
	}
	
	/**
	 * Methods to help with VDMJ plugin event processing.
	 */
	private boolean pluginLifecycle(List<File> files)
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
	
	private boolean report(List<VDMMessage> messages, AbstractCheckFilesEvent event)
	{
		int nerrs  = 0;
		int nwarns = 0;

		for (VDMMessage m: messages)
		{
			if (m instanceof VDMError && !errors.contains(m))
			{
				errors.add((VDMError)m);
				println(m);
				nerrs++;
			}
			else if (m instanceof VDMWarning && ! warnings.contains(m))
			{
				warnings.add((VDMWarning)m);
				println(m);
				nwarns++;
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
			String title = event.getProperty(AbstractCheckFilesEvent.TITLE);
			String kind = event.getProperty(AbstractCheckFilesEvent.KIND);
			
			if (nerrs > 0 || nwarns > 0)
			{
		   		info(title + " " + objects + " in " + duration + " secs. ");
		   		info(nerrs == 0 ? "No " + kind + " errors" : "Found " + plural(nerrs, kind + " error", "s"));
		  		infoln(nwarns == 0 ? "" : " and " + plural(nwarns, "warning", "s"));
			}
		}
		
		return (nerrs == 0);	// Return "OK" if we can continue (ie. no errors)
	}
}
