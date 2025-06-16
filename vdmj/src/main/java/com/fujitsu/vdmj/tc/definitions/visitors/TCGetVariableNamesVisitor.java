/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.tc.definitions.visitors;

import com.fujitsu.vdmj.tc.definitions.TCAssignmentDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCEqualsDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExternalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImportedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCInheritedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMultiBindListDefinition;
import com.fujitsu.vdmj.tc.definitions.TCNamedTraceDefinition;
import com.fujitsu.vdmj.tc.definitions.TCQualifiedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCRenamedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCThreadDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.definitions.TCUntypedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class TCGetVariableNamesVisitor extends TCDefinitionVisitor<TCNameList, Object>
{
	private TCNameList definitionName(TCDefinition def)
	{
		return new TCNameList(def.name);
	}
	
	private TCNameList applyList(TCDefinitionList defs)
	{
		TCNameList list = new TCNameList();

		if (defs != null)
		{
			for (TCDefinition def: defs)
			{
				list.addAll(def.apply(this, null));
			}
		}
		
		return list;
	}
	
	@Override
	public TCNameList caseDefinition(TCDefinition node, Object arg)
	{
		return new TCNameList();
	}
	
	@Override
	public TCNameList caseAssignmentDefinition(TCAssignmentDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseClassDefinition(TCClassDefinition node, Object arg)
	{
		return applyList(node.definitions);
	}
	
	@Override
	public TCNameList caseEqualsDefinition(TCEqualsDefinition node, Object arg)
	{
		return applyList(node.defs);
	}
	
	@Override
	public TCNameList caseExplicitFunctionDefinition(TCExplicitFunctionDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseImplicitFunctionDefinition(TCImplicitFunctionDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseExplicitOperationDefinition(TCExplicitOperationDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseImplicitOperationDefinition(TCImplicitOperationDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseExternalDefinition(TCExternalDefinition node, Object arg)
	{
		return node.state.apply(this, arg);
	}
	
	@Override
	public TCNameList caseImportedDefinition(TCImportedDefinition node, Object arg)
	{
		return node.def.apply(this, arg);
	}
	
	@Override
	public TCNameList caseInheritedDefinition(TCInheritedDefinition node, Object arg)
	{
		TCNameList names = new TCNameList();
		node.checkSuperDefinition();

		for (TCNameToken vn: node.superdef.apply(this, arg))
		{
			names.add(vn.getModifiedName(node.name.getModule()));
		}

		return names;
	}
	
	@Override
	public TCNameList caseLocalDefinition(TCLocalDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseMultiBindListDefinition(TCMultiBindListDefinition node, Object arg)
	{
		return applyList(node.defs);
	}
	
	@Override
	public TCNameList caseNamedTraceDefinition(TCNamedTraceDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseQualifiedDefinition(TCQualifiedDefinition node, Object arg)
	{
		return definitionName(node.def);
	}
	
	@Override
	public TCNameList caseRenamedDefinition(TCRenamedDefinition node, Object arg)
	{
		TCNameList both = new TCNameList(node.name);
		both.add(node.def.name);
		return both;
	}
	
	@Override
	public TCNameList caseStateDefinition(TCStateDefinition node, Object arg)
	{
		return applyList(node.statedefs);
	}
	
	@Override
	public TCNameList caseThreadDefinition(TCThreadDefinition node, Object arg)
	{
		return definitionName(node.operationDef);
	}
	
	@Override
	public TCNameList caseTypeDefinition(TCTypeDefinition node, Object arg)
	{
		// This is only used in VDM++ type inheritance
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseUntypedDefinition(TCUntypedDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseValueDefinition(TCValueDefinition node, Object arg)
	{
		return node.pattern.getVariableNames();
	}
}
