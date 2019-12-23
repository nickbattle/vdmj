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

package com.fujitsu.vdmj.po.definitions;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.po.PORecursiveLoops;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A class for holding a list of ClassDefinitions.
 */
public class POClassList extends POMappedList<TCClassDefinition, POClassDefinition>
{
	private static final long serialVersionUID = 1L;

	public POClassList()
	{
		super();
	}

	public POClassList(TCClassList from) throws Exception
	{
		super(from);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (POClassDefinition c: this)
		{
			sb.append(c.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	public ProofObligationList getProofObligations()
	{
		ProofObligationList obligations = new ProofObligationList();
		
		setRecursiveLoops();

		for (POClassDefinition c: this)
		{
			obligations.addAll(c.getProofObligations(new POContextStack()));
		}

		obligations.trivialCheck();
		return obligations;
	}

	private void setRecursiveLoops()
	{
		Map<TCNameToken, TCNameSet> callmap = new HashMap<TCNameToken, TCNameSet>();
		
		for (POClassDefinition c: this)
		{
			callmap.putAll(c.getCallMap());
		}
		
		PORecursiveLoops.reset();
		
		for (TCNameToken sought: callmap.keySet())
		{
			Stack<TCNameToken> stack = new Stack<TCNameToken>();
			stack.push(sought);
			
			if (PORecursiveLoops.reachable(sought, callmap.get(sought), callmap, stack))
			{
	    		PORecursiveLoops.put(sought, findDefinitions(stack));
			}
			
			stack.pop();
		}
	}
	
	private PODefinitionList findDefinitions(Stack<TCNameToken> stack)
	{
		PODefinitionList list = new PODefinitionList();
		
		for (TCNameToken name: stack)
		{
			list.add(findDefinition(name));
		}
		
		return list;
	}

	private PODefinition findDefinition(TCNameToken sought)
	{
		for (POClassDefinition clazz: this)
		{
			for (PODefinition def: clazz.definitions)
			{
				if (def.name != null && def.name.equals(sought))
				{
					return def;
				}
			}
		}
		
		return null;
	}
}
