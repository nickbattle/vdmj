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
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.syntax.ClassReader;

/**
 * VDM-PP AST plugin
 */
public class ASTPluginPP extends ASTPlugin
{
	protected ASTClassList astClassList = null;
	
	@Override
	protected List<VDMMessage> syntaxPrepare()
	{
		astClassList = new ASTClassList();
		return null;
	}
	
	@Override
	protected List<VDMMessage> syntaxCheck()
	{
		List<VDMMessage> messages = new Vector<VDMMessage>();
		
		for (File file: files)
		{
			ClassReader cr = null;
			
			try
			{
				LexTokenReader ltr = new LexTokenReader(file, Settings.dialect, Settings.filecharset);
				cr = new ClassReader(ltr);
				astClassList.addAll(cr.readClasses());
			}
			catch (InternalException e)
			{
				messages.addAll(errsOf(e));
			}
			catch (Throwable e)
			{
				messages.addAll(errsOf(e));
			}

			if (cr != null)
			{
				messages.addAll(cr.getErrors());
				messages.addAll(cr.getWarnings());
			}
		}

		return messages;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Collection<?>> T getAST()
	{
		return (T)astClassList;
	}
}
