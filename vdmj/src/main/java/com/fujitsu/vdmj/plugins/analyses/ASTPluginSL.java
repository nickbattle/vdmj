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

import static com.fujitsu.vdmj.plugins.PluginConsole.plural;
import static com.fujitsu.vdmj.plugins.PluginConsole.info;
import static com.fujitsu.vdmj.plugins.PluginConsole.infoln;

import java.io.File;

import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.syntax.ModuleReader;

/**
 * VDM-SL AST plugin
 */
public class ASTPluginSL extends ASTPlugin
{
	private ASTModuleList astModuleList = null;
	
	@Override
	protected <T> T syntaxPrepare()
	{
		astModuleList = new ASTModuleList();
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T> T syntaxCheck()
	{
		int errs = 0;
		int warns = 0;
		double duration = 0;
		
		for (File file: files)
		{
			LexTokenReader ltr = new LexTokenReader(file, Dialect.VDM_SL, filecharset);
			ModuleReader mr = new ModuleReader(ltr);
	   		long before = System.currentTimeMillis();
			astModuleList.addAll(mr.readModules());
	   		long after = System.currentTimeMillis();
	   		duration += (after - before);
			
			if (mr.getErrorCount() > 0)
			{
    			mr.printErrors(Console.out);
    			errs += mr.getErrorCount();
			}

			if (mr.getWarningCount() > 0)
			{
    			mr.printWarnings(Console.out);
    			warns += mr.getWarningCount();
			}
		}
	
   		int count = astModuleList.getModuleNames().size();

   		info("Parsed " + plural(count, "module", "s") + " in " +
   			(double)(duration)/1000 + " secs. ");
   		info(errs == 0 ? "No syntax errors" :
   			"Found " + plural(errs, "syntax error", "s"));
  		infoln(warns == 0 ? "" : " and " +
  			(nowarn ? "suppressed " : "") + plural(warns, "warning", "s"));

		return (T) errors;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Mappable> T getAST()
	{
		return (T)astModuleList;
	}
}
