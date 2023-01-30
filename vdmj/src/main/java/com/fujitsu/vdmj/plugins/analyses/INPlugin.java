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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.RemoteControl;
import com.fujitsu.vdmj.RemoteSimulation;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.events.CheckCompleteEvent;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.plugins.events.Event;
import com.fujitsu.vdmj.runtime.ContextException;

/**
 * IN analysis plugin
 */
abstract public class INPlugin extends AnalysisPlugin implements EventListener
{
	protected boolean interactive;
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
	public void processArgs(List<String> argv)
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			String arg = iter.next();
			
			if (arg.equals("-i"))
			{
				iter.remove();
				interactive = true;
			}
			else if (arg.equals("-e"))
			{
				iter.remove();
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
			}
			else if (arg.equals("-default"))
			{
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
			}
			else if (arg.equals("-log"))
			{
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
			}
    		else if (arg.equals("-pre"))
    		{
    			iter.remove();
    			Settings.prechecks = false;
    		}
    		else if (arg.equals("-post"))
    		{
    			iter.remove();
    			Settings.postchecks = false;
    		}
    		else if (arg.equals("-inv"))
    		{
    			iter.remove();
    			Settings.invchecks = false;
    		}
    		else if (arg.equals("-dtc"))
    		{
    			iter.remove();
    			// NB. Turn off both when no DTC
    			Settings.invchecks = false;
    			Settings.dynamictypechecks = false;
    		}
    		else if (arg.equals("-exceptions"))
    		{
    			iter.remove();
    			Settings.exceptions = true;
    		}
    		else if (arg.equals("-measures"))
    		{
    			iter.remove();
    			Settings.measureChecks = false;
    		}
    		else if (arg.equals("-remote"))
    		{
    			iter.remove();
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
    		}
    		else if (arg.equals("-simulation"))
    		{
    			iter.remove();
    			interactive = false;
    			
    			if (iter.hasNext())
    			{
    				remoteSimulationName = iter.next();
    				iter.remove();
    			}
    			else
    			{
    				fail("-simulation option requires a Java classname");
    			}
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
	
	protected List<VDMError> errsOf(Exception... list)
	{
		List<VDMError> errs = new Vector<VDMError>();
		
		for (Exception e: list)
		{
			if (e instanceof ContextException)
			{
				ContextException ce = (ContextException)e;
				errs.add(new VDMError(ce));
			}
			else
			{
				errs.add(new VDMError(0, e.getMessage(), LexLocation.ANY));
			}
		}
		
		return errs;
	}

	@Override
	public <T> T handleEvent(Event event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			return interpreterPrepare();
		}
		else if (event instanceof CheckCompleteEvent)
		{
			return interpreterInit();
		}
		else
		{
			throw new Exception("Unhandled event: " + event);
		}
	}

	abstract protected <T> T interpreterPrepare();

	abstract protected <T> T interpreterInit();

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
}
