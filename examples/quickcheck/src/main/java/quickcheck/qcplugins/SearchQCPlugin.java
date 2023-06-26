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

package quickcheck.qcplugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.ValueSet;

import quickcheck.QuickCheck;
import quickcheck.visitors.SearchQCVisitor;

public class SearchQCPlugin extends QCPlugin
{
	public SearchQCPlugin(List<String> argv)
	{
		// Remove your "qc" plugin arguments from the list here
	}
	
	@Override
	public String getName()
	{
		return "search";
	}

	@Override
	public boolean hasErrors()
	{
		return false;
	}

	@Override
	public boolean init(QuickCheck qc)
	{
		return true;
	}

	@Override
	public Map<String, ValueSet> getValues(ProofObligation po, INExpression exp, List<INBindingSetter> binds)
	{
		NameValuePairList nvps = po.getCheckedExpression().apply(new SearchQCVisitor(), null);
		HashMap<String, ValueSet> result = new HashMap<String, ValueSet>();
		
		for (NameValuePair pair: nvps)
		{
			for (INBindingSetter bind: binds)
			{
				String key = bind.toString();
				
				// HACK! Only works for single name binds
				if (key.equals(pair.name.getName() + ":" + bind.getType()))	// eg. "a:T" = "a" +":" + "T"
				{
					if (result.containsKey(key))
					{
						ValueSet current = result.get(key);
						current.add(pair.value);
					}
					else
					{
						result.put(key, new ValueSet(pair.value));
					}
					break;
				}
			}
		}
		
		return result;
	}
}
