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

package com.fujitsu.vdmj.po.modules;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.po.PORecursiveLoops;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("serial")
public class POModuleList extends POMappedList<TCModule, POModule>
{
	public POModuleList(TCModuleList from) throws Exception
	{
		super(from);
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}

	public ProofObligationList getProofObligations()
	{
		ProofObligationList obligations = new ProofObligationList();
		
		setRecursiveLoops();

		for (POModule m: this)
		{
			obligations.addAll(m.getProofObligations());
		}

		obligations.trivialCheck();
		return obligations;
	}

	private void setRecursiveLoops()
	{
		Map<TCNameToken, TCNameSet> callmap = new HashMap<TCNameToken, TCNameSet>();
		
		for (POModule m: this)
		{
			callmap.putAll(m.getCallMap());
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
		for (POModule module: this)
		{
			for (PODefinition def: module.defs)
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
