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

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.values.ValueSet;

import quickcheck.QuickCheck;
import quickcheck.strategies.QCStrategy;
import quickcheck.strategies.Results;

public class ExampleQCStrategy extends QCStrategy
{
	private boolean provedResult = false;
	private int errorCount = 0;

	public ExampleQCStrategy(List<String> argv)
	{
		// Remove your "qc" plugin arguments from the list here
		// It's useful to include the strategy name, like "-example:n"
		for (int i=0; i < argv.size(); i++)
		{
			switch (argv.get(i))
			{
				case "-example:proved":
					argv.remove(i);

					if (i < argv.size())
					{
						provedResult = Boolean.parseBoolean(argv.get(i));
						argv.remove(i);
					}
					break;
					
				default:
					if (argv.get(i).startsWith("-example:"))
					{
						errorln("Unknown exmaple option: " + argv.get(i));
						errorln(help());
						errorCount++;
						argv.remove(i);
					}
			}
		}
	}
	
	@Override
	public String getName()
	{
		return "example";	// Can be used with -p <name>
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
	public Results getValues(ProofObligation po, INExpression exp, List<INBindingSetter> binds)
	{
		Map<String, ValueSet> values = new HashMap<String, ValueSet>();
		long before = System.currentTimeMillis();
		
		for (INBindingSetter bind: binds)
		{
			values.put(bind.toString(), new ValueSet());	// ie. nothing, for every bind
		}
		
		return new Results(provedResult, values, System.currentTimeMillis() - before);	// NOTE proved flag!
	}

	@Override
	public String help()
	{
		return getName() + " [-example:proved <bool>]";
	}

	@Override
	public boolean useByDefault()
	{
		return false;	// Not used if no -p options given
	}
}
