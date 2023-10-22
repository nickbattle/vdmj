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

package quickcheck.strategies;

import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;

import java.util.HashMap;
import java.util.List;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.QuickCheck;
import quickcheck.visitors.SearchQCVisitor;

public class SearchQCStrategy extends QCStrategy
{
	private int errorCount = 0;

	public SearchQCStrategy(List<String> argv)
	{
		for (int i=0; i < argv.size(); i++)
		{
			// No plugin arguments yet?

			if (argv.get(i).startsWith("-search:"))
			{
				errorln("Unknown search option: " + argv.get(i));
				errorln(help());
				errorCount ++;
				argv.remove(i);
			}
		}
	}
	
	@Override
	public String getName()
	{
		return "search";
	}

	@Override
	public boolean hasErrors()
	{
		return errorCount > 0;
	}

	@Override
	public boolean init(QuickCheck qc)
	{
		return true;
	}

	@Override
	public Results getValues(ProofObligation po, INExpression exp, List<INBindingSetter> binds, Context ctxt)
	{
		HashMap<String, ValueList> result = new HashMap<String, ValueList>();
		long before = System.currentTimeMillis();

		if (po.isCheckable && po.getCheckedExpression() != null)
		{
			boolean exists = po.getCheckedExpression() instanceof TCExistsExpression;
			NameValuePairList nvps = po.getCheckedExpression().apply(new SearchQCVisitor(exists), null);
			
			for (NameValuePair pair: nvps)
			{
				for (INBindingSetter bind: binds)
				{
					String key = bind.toString();
					
					// HACK! Only works for single name binds
					if (key.equals(pair.name.getName() + ":" + bind.getType()))	// eg. "a:T" = "a" +":" + "T"
					{
						// The naivety of the search may result in value assignments that
						// do not match the type (eg. inside is_(exp, T) => ...). So check.
						
						try
						{
							Value fixed = pair.value.convertTo(bind.getType(), ctxt);
							
							if (result.containsKey(key))
							{
								ValueList current = result.get(key);
								current.add(fixed);
							}
							else
							{
								result.put(key, new ValueList(fixed));
							}
						}
						catch (ValueException e)
						{
							// ignore illegal values
						}
						catch (ContextException e)
						{
							// ignore illegal values
						}

						break;
					}
				}
			}
		}
		
		return new Results(null, result, System.currentTimeMillis() - before);
	}

	@Override
	public String help()
	{
		return getName() + " (no options)";
	}

	@Override
	public boolean useByDefault()
	{
		return true;	// Use if no -s given
	}
}
