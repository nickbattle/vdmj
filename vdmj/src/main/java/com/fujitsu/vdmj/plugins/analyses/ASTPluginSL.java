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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.syntax.ModuleReader;

/**
 * VDM-SL AST plugin
 */
public class ASTPluginSL extends ASTPlugin
{
	protected ASTModuleList astModuleList = null;
	
	@Override
	protected List<VDMMessage> syntaxPrepare()
	{
		astModuleList = new ASTModuleList();
		return null;
	}

	@Override
	protected List<VDMMessage> syntaxCheck()
	{
		List<VDMMessage> messages = new Vector<VDMMessage>();
		
		for (File file: files)
		{
			ModuleReader mr = null;
			
			try
			{
				LexTokenReader ltr = new LexTokenReader(file, Dialect.VDM_SL, Settings.filecharset);
				mr = new ModuleReader(ltr);
				astModuleList.addAll(mr.readModules());
			}
			catch (InternalException e)
			{
				messages.addAll(errsOf(e));
			}
			catch (Throwable e)
			{
				messages.addAll(errsOf(e));
			}

			if (mr != null)
			{
				messages.addAll(mr.getErrors());
				messages.addAll(mr.getWarnings());
			}
		}

		return messages;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Collection<?>> T getAST()
	{
		return (T)astModuleList;
	}
}
