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
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.File;

import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.VDMError;
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
		double duration = 0;
		
		for (File file: files)
		{
			ModuleReader mr = null;
			
			try
			{
				LexTokenReader ltr = new LexTokenReader(file, Dialect.VDM_SL, filecharset);
				mr = new ModuleReader(ltr);
				long before = System.currentTimeMillis();
				astModuleList.addAll(mr.readModules());
				long after = System.currentTimeMillis();
				duration += (after - before);
			}
			catch (InternalException e)
			{
				println(e.toString());
				errors.add(new VDMError(0, e.toString(), LexLocation.ANY));
			}
			catch (Throwable e)
			{
				println(e);
				errors.add(new VDMError(0, e.toString(), LexLocation.ANY));
			}

			if (mr != null && mr.getErrorCount() > 0)
			{
				errors.addAll(mr.getErrors());
    			mr.printErrors(Console.out);
			}

			if (mr != null && mr.getWarningCount() > 0)
			{
				warnings.addAll(mr.getWarnings());
    			mr.printWarnings(Console.out);
			}
		}
	
   		int count = astModuleList.getModuleNames().size();

   		info("Parsed " + plural(count, "module", "s") + " in " +
   			(double)(duration)/1000 + " secs. ");
   		info(errors.isEmpty() ? "No syntax errors" :
   			"Found " + plural(errors.size(), "syntax error", "s"));
  		infoln(warnings.isEmpty() ? "" : " and " +
  			(nowarn ? "suppressed " : "") + plural(warnings.size(), "warning", "s"));

		return (T) errors;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Mappable> T getAST()
	{
		return (T)astModuleList;
	}
}
