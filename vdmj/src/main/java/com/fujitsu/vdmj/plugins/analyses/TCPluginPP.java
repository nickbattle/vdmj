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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.VDMMessage;
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
	protected List<VDMMessage> typeCheckPrepare()
	{
		tcClassList = new TCClassList();
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<VDMMessage> typeCheck()
	{
		ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
		ASTClassList parsedClasses = ast.getAST();
		List<VDMMessage> messages = new Vector<VDMMessage>();

		try
   		{
   			tcClassList = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsedClasses);
   			TypeChecker typeChecker = new ClassTypeChecker(tcClassList);
   			typeChecker.typeCheck();
   		}
		catch (InternalException e)
		{
			messages.addAll(errsOf(e));
		}
		catch (Throwable e)
		{
			messages.addAll(errsOf(e));
		}

		messages.addAll(TypeChecker.getErrors());
		messages.addAll(TypeChecker.getWarnings());

		return messages;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Mappable> T getTC()
	{
		return (T)tcClassList;
	}
}
