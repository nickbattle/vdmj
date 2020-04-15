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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.expressions.INLeafExpressionVisitor;
import com.fujitsu.vdmj.in.statements.INLeafStatementVisitor;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCLeafTypeVisitor;

public class INNameFinder extends INLeafDefinitionVisitor<INDefinition, INDefinitionSet, TCNameToken>
{
	@Override
	protected INDefinitionSet newCollection()
	{
		return new INDefinitionSet();
	}

	@Override
	public INDefinitionSet caseDefinition(INDefinition node, TCNameToken sought)
	{
		if (node.name != null && node.name.equals(sought))
		{
			return new INDefinitionSet(node);
		}

		return newCollection();
	}
	
	@Override
	public INDefinitionSet caseClassDefinition(INClassDefinition node, TCNameToken sought)
	{
		for (INDefinition d: node.definitions)
		{
			INDefinitionSet found = d.apply(this, sought);

			if (!found.isEmpty())
			{
				return found;
			}
		}
		
		return newCollection();
	}
	
	@Override
	public INDefinitionSet caseEqualsDefinition(INEqualsDefinition node, TCNameToken sought)
	{
		if (node.defs != null)
		{
			for (INDefinition def: node.defs)
			{
				INDefinitionSet all = def.apply(this, sought);
				
				if (!all.isEmpty())
				{
					return all;
				}
			}
		}

		return newCollection();
	}
	
	@Override
	public INDefinitionSet caseExplicitFunctionDefinition(INExplicitFunctionDefinition node, TCNameToken sought)
	{
		INDefinitionSet s = caseDefinition(node, sought);
		if (!s.isEmpty()) return s;

		if (node.predef != null)
		{
			s = node.predef.apply(this, sought);
			if (!s.isEmpty()) return s;
		}

		if (node.postdef != null)
		{
			s = node.postdef.apply(this, sought);
			if (!s.isEmpty()) return s;
		}

		return s;	// empty
	}
	
	@Override
	public INDefinitionSet caseExplicitOperationDefinition(INExplicitOperationDefinition node, TCNameToken sought)
	{
		INDefinitionSet s = caseDefinition(node, sought);
		if (!s.isEmpty()) return s;

		if (Settings.dialect == Dialect.VDM_SL || Settings.release == Release.CLASSIC)
		{
			if (node.predef != null)
			{
				s = node.predef.apply(this, sought);
				if (!s.isEmpty()) return s;
			}

			if (node.postdef != null)
			{
				s = node.postdef.apply(this, sought);
				if (!s.isEmpty()) return s;
			}
		}

		return s;	// empty
	}
	
	@Override
	public INDefinitionSet caseImplicitFunctionDefinition(INImplicitFunctionDefinition node, TCNameToken sought)
	{
		INDefinitionSet s = caseDefinition(node, sought);
		if (!s.isEmpty()) return s;

		if (node.predef != null)
		{
			s = node.predef.apply(this, sought);
			if (!s.isEmpty()) return s;
		}

		if (node.postdef != null)
		{
			s = node.postdef.apply(this, sought);
			if (!s.isEmpty()) return s;
		}

		return s;	// empty
	}
	
	@Override
	public INDefinitionSet caseImplicitOperationDefinition(INImplicitOperationDefinition node, TCNameToken sought)
	{
		INDefinitionSet s = caseDefinition(node, sought);
		if (!s.isEmpty()) return s;

		if (Settings.dialect == Dialect.VDM_SL || Settings.release == Release.CLASSIC)
		{
			if (node.predef != null)
			{
				s = node.predef.apply(this, sought);
				if (!s.isEmpty()) return s;
			}

			if (node.postdef != null)
			{
				s = node.postdef.apply(this, sought);
				if (!s.isEmpty()) return s;
			}
		}

		return s;	// empty
	}
	
	@Override
	public INDefinitionSet caseExternalDefinition(INExternalDefinition node, TCNameToken sought)
	{
		if (sought.isOld())
		{
			if (sought.equals(node.oldname)) 
			{
				return new INDefinitionSet(node);
			}
		}
		else if (sought.equals(node.state.name))
		{
			return new INDefinitionSet(node);
		}
		
		return newCollection();
	}
	
	@Override
	public INDefinitionSet caseImportedDefinition(INImportedDefinition node, TCNameToken sought)
	{
		return node.def.apply(this, sought);
	}

	@Override
	public INDefinitionSet caseInheritedDefinition(INInheritedDefinition node, TCNameToken sought)
	{
		// The problem is, when the INInheritedDefinition is created, we
		// don't know its fully qualified name.
		
		if (node.superdef instanceof INInheritedDefinition)
		{
			node.superdef.apply(this, sought);	// Set qualifier?
		}

		node.name.setTypeQualifier(node.superdef.name.getTypeQualifier());

		if (node.name.equals(sought) || node.oldname.equals(sought))
		{
			return new INDefinitionSet(node);
		}

		return newCollection();
	}
	
	@Override
	public INDefinitionSet caseInstanceVariableDefinition(INInstanceVariableDefinition node, TCNameToken sought)
	{
		INDefinitionSet found = caseDefinition(node, sought);
		if (!found.isEmpty()) return found;
		
		if (node.oldname.equals(sought))
		{
			return new INDefinitionSet(node);
		}
		
		return newCollection();
	}
	
	@Override
	public INDefinitionSet caseMultiBindListDefinition(INMultiBindListDefinition node, TCNameToken sought)
	{
		if (node.defs != null)
		{
			for (INDefinition def: node.defs)
			{
				INDefinitionSet all = def.apply(this, sought);
				
				if (!all.isEmpty())
				{
					return all;
				}
			}
		}

		return newCollection();
	}

	@Override
	public INDefinitionSet caseQualifiedDefinition(INQualifiedDefinition node, TCNameToken sought)
	{
		return caseDefinition(node, sought);
	}
	
	@Override
	public INDefinitionSet caseRenamedDefinition(INRenamedDefinition node, TCNameToken sought)
	{
		INDefinitionSet renamed = caseDefinition(node, sought);

		if (!renamed.isEmpty())
		{
			return renamed;
		}
		else
		{
			// Renamed definitions hide the original name
			return newCollection();
		}
	}
	
	@Override
	public INDefinitionSet caseStateDefinition(INStateDefinition node, TCNameToken sought)
	{
		if (node.invdef != null)
		{
			INDefinitionSet s = node.invdef.apply(this, sought);
			if (!s.isEmpty()) return s;
		}

		if (node.initdef != null)
		{
			INDefinitionSet s = node.initdef.apply(this, sought);
			if (!s.isEmpty()) return s;
		}

		for (INDefinition d: node.statedefs)
		{
			INDefinitionSet def = d.apply(this, sought);

			if (!def.isEmpty())
			{
				return def;
			}
		}

		return newCollection();
	}
	
	@Override
	public INDefinitionSet caseThreadDefinition(INThreadDefinition node, TCNameToken sought)
	{
		return node.operationDef.apply(this, sought);
	}
	
	@Override
	public INDefinitionSet caseTypeDefinition(INTypeDefinition node, TCNameToken sought)
	{
		if (node.invdef != null)
		{
			INDefinitionSet s = node.invdef.apply(this, sought);
			if (!s.isEmpty()) return s;
		}

		if (node.eqdef != null)
		{
			INDefinitionSet s = node.eqdef.apply(this, sought);
			if (!s.isEmpty()) return s;
		}

		if (node.orddef != null)
		{
			INDefinitionSet s = node.orddef.apply(this, sought);
			if (!s.isEmpty()) return s;
		}

		if (node.mindef != null)
		{
			INDefinitionSet s = node.mindef.apply(this, sought);
			if (!s.isEmpty()) return s;
		}

		if (node.maxdef != null)
		{
			INDefinitionSet s = node.maxdef.apply(this, sought);
			if (!s.isEmpty()) return s;
		}

		return newCollection();
	}
	
	@Override
	public INDefinitionSet caseValueDefinition(INValueDefinition node, TCNameToken sought)
	{
		if (node.pattern.getVariableNames().contains(sought))
		{
			return new INDefinitionSet(node);
		}
		else
		{
			return newCollection();
		}
	}
	
	@Override
	protected INLeafExpressionVisitor<INDefinition, INDefinitionSet, TCNameToken> getExpressionVisitor()
	{
		return null;
	}

	@Override
	protected INLeafStatementVisitor<INDefinition, INDefinitionSet, TCNameToken> getStatementVisitor()
	{
		return null;
	}

	@Override
	protected TCLeafTypeVisitor<INDefinition, INDefinitionSet, TCNameToken> getTypeVisitor()
	{
		return null;
	}
}
