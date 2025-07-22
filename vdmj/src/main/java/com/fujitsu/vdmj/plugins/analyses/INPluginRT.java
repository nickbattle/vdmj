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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.plugins.analyses;

import static com.fujitsu.vdmj.plugins.PluginConsole.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import com.fujitsu.vdmj.RemoteSimulation;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.AnalysisEvent;
import com.fujitsu.vdmj.plugins.events.CheckSyntaxEvent;
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
		eventhub.register(CheckSyntaxEvent.class, this);
		eventhub.register(ShutdownEvent.class, this);
	}
	
	@Override
	public List<VDMMessage> handleEvent(AnalysisEvent event) throws Exception
	{
		if (event instanceof CheckSyntaxEvent)
		{
			RemoteSimulation rs = RemoteSimulation.getInstance();
			
			if (rs != null)
			{
				try
				{
					ASTPlugin ast = registry.getPlugin("AST");
					ASTClassList parsedClasses = ast.getAST();
					rs.setup(parsedClasses);
				}
				catch (Throwable ex)
				{
					println("Simulation: " + ex.getMessage());
					return errsOf(ex);
				}
			}

			return null;
		}
		else if (event instanceof ShutdownEvent)
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

	@Override
	protected List<VDMMessage> interpreterInit(boolean interactive)
	{
		if (logfile != null)
		{
    		try
    		{
    			RTLogger.setLogfileName(new File(logfile));
    			printf("Writing RT events to %s\n", logfile);
    		}
    		catch (FileNotFoundException e)
    		{
    			printf("Cannot create RT event log: %s\n", e.getMessage());
    			return errsOf(e);
    		}
		}
		
		return super.interpreterInit(interactive);
	}
}
