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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.definitions.visitors;

import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.POClassInvariantDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POEqualsDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POExternalDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImportedDefinition;
import com.fujitsu.vdmj.po.definitions.POInheritedDefinition;
import com.fujitsu.vdmj.po.definitions.POLocalDefinition;
import com.fujitsu.vdmj.po.definitions.POMultiBindListDefinition;
import com.fujitsu.vdmj.po.definitions.PONamedTraceDefinition;
import com.fujitsu.vdmj.po.definitions.POQualifiedDefinition;
import com.fujitsu.vdmj.po.definitions.PORenamedDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.definitions.POThreadDefinition;
import com.fujitsu.vdmj.po.definitions.POTypeDefinition;
import com.fujitsu.vdmj.po.definitions.POUntypedDefinition;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;

public class POGetVariableNamesVisitor extends PODefinitionVisitor<TCNameList, Object>
{
	private TCNameList definitionName(PODefinition def)
	{
		return new TCNameList(def.name);
	}
	
	private TCNameList applyList(PODefinitionList defs)
	{
		TCNameList list = new TCNameList();

		if (defs != null)
		{
			for (PODefinition def: defs)
			{
				list.addAll(def.apply(this, null));
			}
		}
		
		return list;
	}
	
	@Override
	public TCNameList caseDefinition(PODefinition node, Object arg)
	{
		return new TCNameList();
	}
	
	@Override
	public TCNameList caseAssignmentDefinition(POAssignmentDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseClassDefinition(POClassDefinition node, Object arg)
	{
		return applyList(node.definitions);
	}
	
	@Override
	public TCNameList caseClassInvariantDefinition(POClassInvariantDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseEqualsDefinition(POEqualsDefinition node, Object arg)
	{
		return applyList(node.defs);
	}
	
	@Override
	public TCNameList caseExplicitFunctionDefinition(POExplicitFunctionDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseImplicitFunctionDefinition(POImplicitFunctionDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseExplicitOperationDefinition(POExplicitOperationDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseImplicitOperationDefinition(POImplicitOperationDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseExternalDefinition(POExternalDefinition node, Object arg)
	{
		return node.state.apply(this, arg);
	}
	
	@Override
	public TCNameList caseImportedDefinition(POImportedDefinition node, Object arg)
	{
		return node.def.apply(this, arg);
	}
	
	@Override
	public TCNameList caseInheritedDefinition(POInheritedDefinition node, Object arg)
	{
		TCNameList names = new TCNameList();

		for (TCNameToken vn: node.superdef.apply(this, arg))
		{
			names.add(vn.getModifiedName(node.name.getModule()));
		}

		return names;
	}
	
	@Override
	public TCNameList caseLocalDefinition(POLocalDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseMultiBindListDefinition(POMultiBindListDefinition node, Object arg)
	{
		return applyList(node.defs);
	}
	
	@Override
	public TCNameList caseNamedTraceDefinition(PONamedTraceDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseQualifiedDefinition(POQualifiedDefinition node, Object arg)
	{
		return definitionName(node.def);
	}
	
	@Override
	public TCNameList caseRenamedDefinition(PORenamedDefinition node, Object arg)
	{
		TCNameList both = new TCNameList(node.name);
		both.add(node.def.name);
		return both;
	}
	
	@Override
	public TCNameList caseStateDefinition(POStateDefinition node, Object arg)
	{
		TCNameList names = new TCNameList();
		
		for (TCField field: node.fields)
		{
			names.add(field.tagname);
		}
		
		return names;
	}
	
	@Override
	public TCNameList caseThreadDefinition(POThreadDefinition node, Object arg)
	{
		return new TCNameList(node.operationName);
	}
	
	@Override
	public TCNameList caseTypeDefinition(POTypeDefinition node, Object arg)
	{
		// This is only used in VDM++ type inheritance
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseUntypedDefinition(POUntypedDefinition node, Object arg)
	{
		return definitionName(node);
	}
	
	@Override
	public TCNameList caseValueDefinition(POValueDefinition node, Object arg)
	{
		return node.pattern.getVariableNames();
	}
}
