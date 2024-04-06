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

import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static quickcheck.commands.QCConsole.verbose;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.QuickCheck;
import quickcheck.visitors.SearchQCVisitor;

public class SearchQCStrategy extends QCStrategy
{
	private int errorCount = 0;

	public SearchQCStrategy(List<?> argv)
	{
		if (!argv.isEmpty() && argv.get(0) instanceof String)
		{
			for (int i=0; i < argv.size(); i++)
			{
				String arg = (String)argv.get(i);
			
				if (arg.startsWith("-search:"))
				{
					println("Unknown search option: " + arg);
					println(help());
					errorCount ++;
					argv.remove(i);
				}
			}
		}
		else
		{
			@SuppressWarnings({ "unchecked", "unused" })
			Map<String, Object> map = getParams((List<Map<String, Object>>) argv, "search");
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
	public StrategyResults getValues(ProofObligation po, List<INBindingOverride> binds, Context ctxt)
	{
		HashMap<String, ValueList> result = new HashMap<String, ValueList>();
		long before = System.currentTimeMillis();

		if (po.isCheckable && po.getCheckedExpression() != null)
		{
			boolean exists = po.isExistential();
			NameValuePairList nvps = po.getCheckedExpression().apply(new SearchQCVisitor(exists), null);
			
			for (NameValuePair pair: nvps)
			{
				for (INBindingOverride bind: binds)
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
			
			if (nvps.isEmpty())
			{
				verbose("No search patterns found\n");
			}
		}
		
		return new StrategyResults(result, false, System.currentTimeMillis() - before);
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
