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
import java.util.Collection;
import java.util.List;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.in.statements.INStatementList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.util.Utils;

/**
 * VDM-SL IN plugin
 */
public class INPluginSL extends INPlugin
{
	protected INModuleList inModuleList = null;
	protected ModuleInterpreter interpreter = null;
	
	@Override
	public List<VDMMessage> interpreterPrepare()
	{
		inModuleList = new INModuleList();
		interpreter = null;
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Collection<?>> T getIN()
	{
		return (T)inModuleList;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Interpreter> T getInterpreter()
	{
		if (interpreter == null)
		{
			try
			{
				TCPlugin tc = registry.getPlugin("TC");
				TCModuleList checkedModules = tc.getTC();
				long before = System.currentTimeMillis();
				inModuleList = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checkedModules);
				Utils.mapperStats(before, INNode.MAPPINGS);
				interpreter = new ModuleInterpreter(inModuleList, checkedModules);
			}
			catch (Exception e)
			{
				println(e);
				fail("Cannot create interpreter");
			}
		}
		
		return (T)interpreter;
	}

	@Override
	public INExpressionList findExpressions(File file, int lineno)
	{
		return inModuleList.findExpressions(file, lineno);
	}

	@Override
	public INStatementList findStatements(File file, int lineno)
	{
		return inModuleList.findStatements(file, lineno);
	}
}
