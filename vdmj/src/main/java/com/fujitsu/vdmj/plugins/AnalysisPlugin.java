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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.runtime.ContextException;

/**
 * The abstract root of all VDMJ analysis plugins.
 */
abstract public class AnalysisPlugin
{
	protected final EventHub eventhub;
	
	protected AnalysisPlugin()
	{
		eventhub = EventHub.getInstance();
	}
	
	public abstract String getName();
	public abstract void init();
	
	public void processArgs(List<String> argv)
	{
		return;			// Process any command line -options used
	}
	
	public void usage()
	{
		return;			// List usage of any command line -options used
	}
	
	public AnalysisCommand getCommand(String[] argv)
	{
		return null;	// Get an object to handle argv, if supported
	}
	
	public void help()
	{
		return;			// List usage of commands supported
	}

	protected List<VDMMessage> errsOf(Throwable e)
	{
		List<VDMMessage> errs = new Vector<VDMMessage>();
		
		if (e instanceof ContextException)
		{
			ContextException ce = (ContextException)e;
			errs.add(new VDMError(ce));
		}
		else
		{
			errs.add(new VDMError(0, e.getMessage(), LexLocation.ANY));
		}
		
		return errs;
	}
	
	protected AnalysisCommand lookup(String[] argv, CommandList commands)
	{
		for (Class<? extends AnalysisCommand> cmd: commands)
		{
			try
			{
				Constructor<? extends AnalysisCommand> ctor = cmd.getConstructor(String[].class);
				return ctor.newInstance(new Object[]{argv});
			}
			catch (InvocationTargetException e)
			{
				if (e.getCause() instanceof IllegalArgumentException)
				{
					// doesn't match argv, try next one
				}
				else if (e.getCause() instanceof NoSuchMethodException)
				{
					printf("%s does not implement constructor(String[] argv)?\n", cmd.getSimpleName());
				}
			}
			catch (Throwable e)
			{
				println("Command " + cmd.getSimpleName() + ": " + e.getMessage());
			}
		}
		
		return null;
	}
	
	protected void showHelp(CommandList commands)
	{
		for (Class<? extends AnalysisCommand> cmd: commands)
		{
			try
			{
				Method help = cmd.getMethod("help", (Class<?>[])null);
				
				if ((help.getModifiers() & Modifier.STATIC) != 0)
				{
					help.invoke(null, (Object[])null);
				}
				else
				{
					printf("%s does not implement static void help()?\n", cmd.getSimpleName());
				}
			}
			catch (Throwable e)
			{
				// Should never happen
			}
		}
	}
}
