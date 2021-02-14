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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class INNameFinder extends INDefinitionVisitor<INDefinition, TCNameToken>
{
	@Override
	public INDefinition caseDefinition(INDefinition node, TCNameToken sought)
	{
		if (node.name != null && node.name.equals(sought))
		{
			return node;
		}

		return null;
	}
	
	@Override
	public INDefinition caseClassDefinition(INClassDefinition node, TCNameToken sought)
	{
		for (INDefinition d: node.definitions)
		{
			INDefinition found = d.apply(this, sought);

			if (found != null)
			{
				return found;
			}
		}
		
		return null;
	}
	
	@Override
	public INDefinition caseEqualsDefinition(INEqualsDefinition node, TCNameToken sought)
	{
		if (node.defs != null)
		{
			for (INDefinition def: node.defs)
			{
				INDefinition all = def.apply(this, sought);
				
				if (all != null)
				{
					return all;
				}
			}
		}

		return null;
	}
	
	@Override
	public INDefinition caseExplicitFunctionDefinition(INExplicitFunctionDefinition node, TCNameToken sought)
	{
		INDefinition s = caseDefinition(node, sought);
		if (s != null) return s;

		if (node.predef != null)
		{
			s = node.predef.apply(this, sought);
			if (s != null) return s;
		}

		if (node.postdef != null)
		{
			s = node.postdef.apply(this, sought);
			if (s != null) return s;
		}

		return s;	// empty
	}
	
	@Override
	public INDefinition caseExplicitOperationDefinition(INExplicitOperationDefinition node, TCNameToken sought)
	{
		INDefinition s = caseDefinition(node, sought);
		if (s != null) return s;

		if (Settings.dialect == Dialect.VDM_SL || Settings.release == Release.CLASSIC)
		{
			if (node.predef != null)
			{
				s = node.predef.apply(this, sought);
				if (s != null) return s;
			}

			if (node.postdef != null)
			{
				s = node.postdef.apply(this, sought);
				if (s != null) return s;
			}
		}

		return s;	// empty
	}
	
	@Override
	public INDefinition caseImplicitFunctionDefinition(INImplicitFunctionDefinition node, TCNameToken sought)
	{
		INDefinition s = caseDefinition(node, sought);
		if (s != null) return s;

		if (node.predef != null)
		{
			s = node.predef.apply(this, sought);
			if (s != null) return s;
		}

		if (node.postdef != null)
		{
			s = node.postdef.apply(this, sought);
			if (s != null) return s;
		}

		return s;	// empty
	}
	
	@Override
	public INDefinition caseImplicitOperationDefinition(INImplicitOperationDefinition node, TCNameToken sought)
	{
		INDefinition s = caseDefinition(node, sought);
		if (s != null) return s;

		if (Settings.dialect == Dialect.VDM_SL || Settings.release == Release.CLASSIC)
		{
			if (node.predef != null)
			{
				s = node.predef.apply(this, sought);
				if (s != null) return s;
			}

			if (node.postdef != null)
			{
				s = node.postdef.apply(this, sought);
				if (s != null) return s;
			}
		}

		return s;	// empty
	}
	
	@Override
	public INDefinition caseExternalDefinition(INExternalDefinition node, TCNameToken sought)
	{
		if (sought.isOld())
		{
			if (sought.equals(node.oldname)) 
			{
				return node;
			}
		}
		else if (sought.equals(node.state.name))
		{
			return node;
		}
		
		return null;
	}
	
	@Override
	public INDefinition caseImportedDefinition(INImportedDefinition node, TCNameToken sought)
	{
		return node.def.apply(this, sought);
	}

	@Override
	public INDefinition caseInheritedDefinition(INInheritedDefinition node, TCNameToken sought)
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
			return node;
		}

		return null;
	}
	
	@Override
	public INDefinition caseInstanceVariableDefinition(INInstanceVariableDefinition node, TCNameToken sought)
	{
		INDefinition found = caseDefinition(node, sought);
		if (found != null) return found;
		
		if (node.oldname.equals(sought))
		{
			return node;
		}
		
		return null;
	}
	
	@Override
	public INDefinition caseMultiBindListDefinition(INMultiBindListDefinition node, TCNameToken sought)
	{
		if (node.defs != null)
		{
			for (INDefinition def: node.defs)
			{
				INDefinition all = def.apply(this, sought);
				
				if (all != null)
				{
					return all;
				}
			}
		}

		return null;
	}

	@Override
	public INDefinition caseQualifiedDefinition(INQualifiedDefinition node, TCNameToken sought)
	{
		return caseDefinition(node, sought);
	}
	
	@Override
	public INDefinition caseRenamedDefinition(INRenamedDefinition node, TCNameToken sought)
	{
		INDefinition renamed = caseDefinition(node, sought);

		if (renamed != null)
		{
			return renamed;
		}
		else
		{
			// Renamed definitions hide the original name
			return null;
		}
	}
	
	@Override
	public INDefinition caseStateDefinition(INStateDefinition node, TCNameToken sought)
	{
		if (node.invdef != null)
		{
			INDefinition s = node.invdef.apply(this, sought);
			if (s != null) return s;
		}

		if (node.initdef != null)
		{
			INDefinition s = node.initdef.apply(this, sought);
			if (s != null) return s;
		}

		for (INDefinition d: node.statedefs)
		{
			INDefinition def = d.apply(this, sought);

			if (def != null)
			{
				return def;
			}
		}

		return null;
	}
	
	@Override
	public INDefinition caseThreadDefinition(INThreadDefinition node, TCNameToken sought)
	{
		return node.operationDef.apply(this, sought);
	}
	
	@Override
	public INDefinition caseTypeDefinition(INTypeDefinition node, TCNameToken sought)
	{
		if (node.invdef != null)
		{
			INDefinition s = node.invdef.apply(this, sought);
			if (s != null) return s;
		}

		if (node.eqdef != null)
		{
			INDefinition s = node.eqdef.apply(this, sought);
			if (s != null) return s;
		}

		if (node.orddef != null)
		{
			INDefinition s = node.orddef.apply(this, sought);
			if (s != null) return s;
		}

		if (node.mindef != null)
		{
			INDefinition s = node.mindef.apply(this, sought);
			if (s != null) return s;
		}

		if (node.maxdef != null)
		{
			INDefinition s = node.maxdef.apply(this, sought);
			if (s != null) return s;
		}

		return null;
	}
	
	@Override
	public INDefinition caseValueDefinition(INValueDefinition node, TCNameToken sought)
	{
		if (node.pattern.getVariableNames().contains(sought))
		{
			return node;
		}
		else
		{
			return null;
		}
	}
}
