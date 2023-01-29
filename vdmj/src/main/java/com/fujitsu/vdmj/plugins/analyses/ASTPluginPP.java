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

import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.plugins.events.CheckSyntaxEvent;
import com.fujitsu.vdmj.syntax.ClassReader;

/**
 * VDM-PP AST plugin
 */
public class ASTPluginPP extends ASTPlugin
{
	private ASTClassList astClassList = null;
	
	@Override
	protected <T> T syntaxPrepare()
	{
		astClassList = new ASTClassList();
		return null;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected <T> T syntaxCheck(CheckSyntaxEvent event)
	{
		for (File file: files)
		{
			LexTokenReader ltr = new LexTokenReader(file, Dialect.VDM_PP);
			ClassReader mr = new ClassReader(ltr);
			astClassList.addAll(mr.readClasses());
			
			if (mr.getErrorCount() > 0)
			{
				errors.addAll(mr.getErrors());
			}
			
			if (mr.getWarningCount() > 0)
			{
				warnings.addAll(mr.getWarnings());
			}
		}
	
		return (T) Boolean.valueOf(!errors.isEmpty());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Mappable> T getAST()
	{
		return (T)astClassList;
	}
}
