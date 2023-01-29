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

import static com.fujitsu.vdmj.plugins.PluginConsole.info;
import static com.fujitsu.vdmj.plugins.PluginConsole.infoln;
import static com.fujitsu.vdmj.plugins.PluginConsole.plural;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * VDM-PP TC plugin
 */
public class TCPluginPP extends TCPlugin
{
	private TCClassList tcClassList = null;
	
	@Override
	protected <T> T typeCheckPrepare()
	{
		tcClassList = new TCClassList();
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T> T typeCheck()
	{
		long before = System.currentTimeMillis();
		int terrs = 0;
		ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
		ASTClassList parsedClasses = ast.getAST();

		try
   		{
   			tcClassList = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsedClasses);
   			TypeChecker typeChecker = new ClassTypeChecker(tcClassList);
   			typeChecker.typeCheck();
   		}
		catch (InternalException e)
		{
			println(e.toString());
		}
		catch (Throwable e)
		{
			println(e);
			terrs++;
		}

   		long after = System.currentTimeMillis();
		terrs += TypeChecker.getErrorCount();

		if (terrs > 0)
		{
			TypeChecker.printErrors(Console.out);
		}

  		int twarn = TypeChecker.getWarningCount();

		if (twarn > 0 && !nowarn)
		{
			TypeChecker.printWarnings(Console.out);
		}

		info("Type checked " + plural(tcClassList.size(), "class", "es") +
			" in " + (double)(after-before)/1000 + " secs. ");
  		info(terrs == 0 ? "No type errors" :
  			"Found " + plural(terrs, "type error", "s"));
  		infoln(twarn == 0 ? "" : " and " +
  			(nowarn ? "suppressed " : "") + plural(twarn, "warning", "s"));

		return (T) errors;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Mappable> T getTC()
	{
		return (T)tcClassList;
	}
}
