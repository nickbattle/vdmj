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

package quickcheck.example;

import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.QuickCheck;
import quickcheck.strategies.QCStrategy;
import quickcheck.strategies.StrategyResults;

public class ExampleQCStrategy extends QCStrategy
{
	private boolean provedResult = false;
	private int errorCount = 0;

	@SuppressWarnings("unchecked")
	public ExampleQCStrategy(List<?> argv)
	{
		if (!argv.isEmpty() && argv.get(0) instanceof String)
		{
			List<String> args = (List<String>)argv;
			
			// Remove your "qc" plugin arguments from the list here
			// It's useful to include the strategy name, like "-example:n"
			for (int i=0; i < args.size(); i++)
			{
				switch (args.get(i))
				{
					case "-example:proved":
						args.remove(i);
	
						if (i < args.size())
						{
							provedResult = Boolean.parseBoolean(args.get(i));
							args.remove(i);
						}
						break;
						
					default:
						if (args.get(i).startsWith("-example:"))
						{
							errorln("Unknown exmaple option: " + args.get(i));
							errorln(help());
							errorCount++;
							args.remove(i);
						}
				}
			}
		}
		else
		{
			Map<String, Object> map = getParams((List<Map<String, Object>>) argv, "example");
			provedResult = get(map, "proved", false);
		}
	}
	
	@Override
	public String getName()
	{
		return "example";	// Can be used with -s <name>
	}

	@Override
	public boolean hasErrors()
	{
		return errorCount > 0;	// Called after init and getValues
	}

	@Override
	public boolean init(QuickCheck qc)
	{
		return true;	// Return value => whether to do checks or stop 
	}

	@Override
	public StrategyResults getValues(ProofObligation po, List<INBindingOverride> binds, Context ctxt)
	{
		Map<String, ValueList> values = new HashMap<String, ValueList>();
		long before = System.currentTimeMillis();
		
		for (INBindingOverride bind: binds)
		{
			values.put(bind.toString(), new ValueList());	// ie. nothing, for every bind
		}
		
		if (provedResult)
		{
			Context witness = null;		// Could have witness values set
			return new StrategyResults(getName(), "Just an example", witness , System.currentTimeMillis() - before);
		}
		else
		{
			return new StrategyResults(values, false, System.currentTimeMillis() - before);
		}
	}

	@Override
	public String help()
	{
		return getName() + " [-example:proved <bool>]";
	}

	@Override
	public boolean useByDefault()
	{
		return false;	// Not used if no -s options given
	}
}
