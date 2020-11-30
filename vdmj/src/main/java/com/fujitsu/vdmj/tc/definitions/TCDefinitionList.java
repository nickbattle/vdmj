/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.tc.TCMappedList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A class to hold a list of Definitions.
 */
@SuppressWarnings("serial")
public class TCDefinitionList extends TCMappedList<ASTDefinition, TCDefinition>
{
	public TCDefinitionList(ASTDefinitionList from) throws Exception
	{
		super(from);
	}

	public TCDefinitionList()
	{
		super();
	}

	public TCDefinitionList(TCDefinition definition)
	{
		add(definition);
	}

	public void implicitDefinitions(Environment env)
	{
		for (TCDefinition d: this)
		{
			d.implicitDefinitions(env);
		}
	}

	public TCDefinitionList singleDefinitions()
	{
		TCDefinitionList all = new TCDefinitionList();

		for (TCDefinition d: this)
		{
			all.addAll(d.getDefinitions());
		}

		return all;
	}

	public void typeCheck(Environment env, NameScope scope)
	{
		for (TCDefinition d: this)
		{
			if (d.name != null && d.name.toString().equals("RESULT"))
			{
				d.report(3336, "Illegal use of RESULT reserved identifier");
			}
			
			d.typeCheck(env, scope);
		}
	}

	public void typeResolve(Environment env)
	{
		for (TCDefinition d: this)
		{
			d.typeResolve(env);
		}
	}

	public void unusedCheck()
	{
		for (TCDefinition d: this)
		{
			d.unusedCheck();
		}
	}

	public TCDefinition findName(TCNameToken name, NameScope scope)
	{
		for (TCDefinition d: this)
		{
			TCDefinition def = d.findName(name, scope);

			if (def != null)
			{
				return def;
			}
		}

		return null;
	}

	public TCDefinition findType(TCNameToken name, String fromModule)
	{
		for (TCDefinition d: this)
		{
			TCDefinition def = d.findType(name, fromModule);

			if (def != null)
			{
				return def;
			}
		}

		return null;
	}

	public TCStateDefinition findStateDefinition()
	{
   		for (TCDefinition d: this)
		{
			if (d instanceof TCStateDefinition)
			{
				return (TCStateDefinition)d;
			}
		}

   		return null;
	}

	public TCNameList getVariableNames()
	{
		TCNameList variableNames = new TCNameList();

		for (TCDefinition d: this)
		{
			variableNames.addAll(d.getVariableNames());
		}

		return variableNames;
	}

	public void setAccessibility(TCAccessSpecifier access)
	{
		for (TCDefinition d: this)
		{
			d.setAccessSpecifier(access);
		}
	}

	public void setClassDefinition(TCClassDefinition def)
	{
		for (TCDefinition d: this)
		{
			d.setClassDefinition(def);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (TCDefinition d: this)
		{
			sb.append(d.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	public TCDefinitionSet findMatches(TCNameToken name)
	{
		TCDefinitionSet set = new TCDefinitionSet();

		for (TCDefinition d: singleDefinitions())
		{
			if (d.isFunctionOrOperation() && d.name.matches(name))
			{
				set.add(d);
			}
		}

		return set;
	}

	public void markUsed()
    {
		for (TCDefinition d: this)
		{
			d.markUsed();
		}
    }

	public void initializedCheck()
	{
		for (TCDefinition d: this)
		{
			if (d instanceof TCInstanceVariableDefinition)
			{
				TCInstanceVariableDefinition ivd = (TCInstanceVariableDefinition)d;
				ivd.initializedCheck();
			}
		}
 	}

	public boolean hasSubclassResponsibility()
	{
		for (TCDefinition d: this)
		{
			if (d.isSubclassResponsibility())
			{
				return true;
			}
		}

		return false;
	}
	
	public void removeDuplicates()
	{
		TCDefinitionList fixed = new TCDefinitionList();
		
		for (TCDefinition d: this)
		{
			d.findName(d.name, NameScope.NAMESANDSTATE);	// Update TCInheritedDefinition type qualifiers
			
			if (!fixed.contains(d))		// Name comparison
			{
				fixed.add(d);
			}
		}
		
		if (fixed.size() < size())
		{
			clear();
			addAll(fixed);
		}
	}

	public void setExcluded(boolean ex)
	{
		for (TCDefinition d: this)
		{
			d.excluded = ex;
		}
	}
	
	public TCDefinitionList removeAbstracts()
	{
		TCDefinitionList keep = new TCDefinitionList();
		
		for (TCDefinition def: this)
		{
			if (def.isSubclassResponsibility())
			{
				boolean found = false;
				
				for (TCDefinition def2: this)
				{
					def2.findName(def2.name, NameScope.NAMESANDSTATE);	// Update type qualifier
					
					if (!def2.isSubclassResponsibility() &&
						def.findName(def2.name, NameScope.NAMESANDSTATE) != null)
					{
						found = true;
						break;
					}
				}
				
				if (!found)
				{
					keep.add(def);
				}
			}
			else
			{
				keep.add(def);
			}
		}
		
		return keep;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof TCDefinitionList)
		{
			return super.equals(other);
		}
		
		return false;
	}
}
