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

import static com.fujitsu.vdmj.plugins.PluginConsole.fail;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.debug.ConsoleKeyWatcher;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INStatementList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.util.Utils;

/**
 * VDM-PP IN plugin
 */
public class INPluginPP extends INPlugin
{
	protected INClassList inClassList = null;
	protected ClassInterpreter interpreter = null;
	
	@Override
	protected List<VDMMessage> interpreterPrepare()
	{
		inClassList = new INClassList();
		interpreter = null;
		return null;
	}

	@Override
	protected List<VDMMessage> interpreterInit()
	{
		try
		{
   			getInterpreter();
   			ConsoleDebugReader dbg = null;
   			ConsoleKeyWatcher watcher = null;

   			try
   			{
   				if (interactive)
   				{
	   				dbg = new ConsoleDebugReader();
	   				dbg.start();
	   				watcher = new ConsoleKeyWatcher("init");
	   				watcher.start();
   				}
   				
   				interpreter.init();
   			}
   			finally
   			{
   				if (dbg != null)
   				{
   					dbg.interrupt();
   				}
   				
   				if (watcher != null)
   				{
   					watcher.interrupt();
   				}
   			}

   			if (defaultName != null)
   			{
   				interpreter.setDefaultName(defaultName);
   			}
		}
		catch (ContextException e)
		{
			println("Initialization: " + e);
			
			if (e.isStackOverflow())
			{
				e.ctxt.printStackFrames(Console.out);
			}
			else
			{
				e.ctxt.printStackTrace(Console.out, true);
			}
			
			return errsOf(e);
		}
		catch (Throwable e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Initialization:");
			println(e);

			return errsOf(e);
		}
		
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Collection<?>> T getIN()
	{
		return (T)inClassList;
	}

	@Override
	public ClassInterpreter getInterpreter()
	{
		if (interpreter == null)
		{
			try
			{
				TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
				TCClassList checkedClasses = tc.getTC();
				long before = System.currentTimeMillis();
				inClassList = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checkedClasses);
				Utils.mapperStats(before, INNode.MAPPINGS);
				interpreter = new ClassInterpreter(inClassList, checkedClasses);
			}
			catch (Exception e)
			{
				println(e);
				fail("Cannot create interpreter");
			}
		}
		
		return interpreter;
	}

	@Override
	public INExpressionList findExpressions(File file, int lineno)
	{
		return inClassList.findExpressions(file, lineno);
	}

	@Override
	public INStatementList findStatements(File file, int lineno)
	{
		return inClassList.findStatements(file, lineno);
	}
}
