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

import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * VDM-SL TC plugin
 */
public class TCPluginSL extends TCPlugin
{
	private TCModuleList tcModuleList = null;
	
	@Override
	protected List<VDMMessage> typeCheckPrepare()
	{
		tcModuleList = new TCModuleList();
		return null;
	}

	@Override
	protected List<VDMMessage> typeCheck()
	{
		ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
		ASTModuleList parsedModules = ast.getAST();
		List<VDMMessage> messages = new Vector<VDMMessage>();

		try
   		{
   			tcModuleList = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsedModules);
   			tcModuleList.combineDefaults();
   			TypeChecker typeChecker = new ModuleTypeChecker(tcModuleList);
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
		return (T)tcModuleList;
	}
}
