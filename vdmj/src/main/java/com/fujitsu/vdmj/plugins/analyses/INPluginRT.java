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

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.File;
import java.io.FileNotFoundException;

import com.fujitsu.vdmj.RemoteSimulation;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.events.Event;
import com.fujitsu.vdmj.plugins.events.ShutdownEvent;

/**
 * VDM-RT IN plugin
 */
public class INPluginRT extends INPluginPP
{
	@Override
	public void init()
	{
		super.init();
		eventhub.register(ShutdownEvent.class, this);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T handleEvent(Event event) throws Exception
	{
		if (event instanceof ShutdownEvent)
		{
			if (RTLogger.getLogSize() > 0)
			{
				println("Closing RT event log");
				RTLogger.dump(true);
			}

			return null;
		}
		else
		{
			return super.handleEvent(event);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected <T> T interpreterPrepare()
	{
		super.interpreterPrepare();
		
		RemoteSimulation rs = RemoteSimulation.getInstance();
		
		if (rs != null)
		{
			try
			{
				ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
				ASTClassList parsedClasses = ast.getAST();
				rs.setup(parsedClasses);
			}
			catch (Exception ex)
			{
				println("Simulation: " + ex.getMessage());
				return (T) errsOf(ex);
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T> T interpreterInit()
	{
		if (logfile != null)
		{
    		try
    		{
    			RTLogger.setLogfileName(new File(logfile));
    			println("Writing RT events to " + logfile);
    		}
    		catch (FileNotFoundException e)
    		{
    			println("Cannot create RT event log: " + e.getMessage());
    			return (T) errsOf(e);
    		}
		}
		
		return super.interpreterInit();
	}
}
