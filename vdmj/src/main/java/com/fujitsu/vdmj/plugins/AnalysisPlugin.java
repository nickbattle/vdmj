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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.Dialect;
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
	protected final PluginRegistry registry;
	
	public static AnalysisPlugin factory(Dialect dialect) throws Exception
	{
		throw new Exception("Plugin must provide a static factory(Dialect) method");
	}
	
	protected AnalysisPlugin()
	{
		eventhub = EventHub.getInstance();
		registry = PluginRegistry.getInstance();
	}
	
	public abstract String getName();
	public abstract void init();
	
	/**
	 * The priority affects the order that plugins are sent events via the EventHub.
	 * Lower priorities are sent first. The system plugin priorities are fixed multiples
	 * of 100. User plugins are typically later, but can be earlier. If a plugin does
	 * not define a priority, they get the default, which effectively means classpath
	 * order.
	 */
	public int getPriority()
	{
		return 1000;	// Default user plugin priority
	}
	
	/**
	 * This calls the getPriority defined above, and is used by the EventHub to order
	 * EventListeners for event publication.
	 */
	public int compareTo(EventListener other)
	{
		return getPriority() - other.getPriority();
	}
	
	public String getDescription()
	{
		return getClass().getName() + ", priority " + getPriority();
	}
	
	public void processArgs(List<String> argv)
	{
		return;			// Process any command line -options used
	}
	
	public void usage()
	{
		return;			// List usage of any command line -options used
	}
	
	public AnalysisCommand getCommand(String line)
	{
		return null;	// Get an object to handle line, if supported
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
}
