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

import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static com.fujitsu.vdmj.plugins.PluginConsole.verbose;

import java.util.HashMap;
import java.util.List;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.values.ValueSet;

import quickcheck.QuickCheck;
import quickcheck.visitors.RandomRangeCreator;

public class RandomQCPlugin extends QCPlugin
{
	private int numSetSize = 5;			// ie. size of sets for numeric types
	private int expansionLimit = 20;	// Overall returned value limit

	public RandomQCPlugin(List<String> argv)
	{
		for (int i=0; i < argv.size(); i++)
		{
			try
			{
				switch (argv.get(i))
				{
					case "-random:n":
						argv.remove(i);

						if (i < argv.size())
						{
							numSetSize = Integer.parseInt(argv.get(i));
							argv.remove(i);
						}
						break;
						
					case "-random:s":		// Total top level size
						argv.remove(i);

						if (i < argv.size())
						{
							expansionLimit = Integer.parseInt(argv.get(i));
							argv.remove(i);
						}
						break;
				}
			}
			catch (NumberFormatException e)
			{
				println("Argument must be numeric");
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				println("Missing argument");
			}
		}
		
		verbose("random:n = %d\n", numSetSize);
		verbose("random:s = %d\n", expansionLimit);
	}
	
	@Override
	public String getName()
	{
		return "random";
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
	public Results getValues(ProofObligation po, INExpression exp, List<INBindingSetter> binds)
	{
		HashMap<String, ValueSet> result = new HashMap<String, ValueSet>();
		RootContext ctxt = Interpreter.getInstance().getInitialContext();
		long seed = 1234;
		
		for (INBindingSetter bind: binds)
		{
			RandomRangeCreator visitor = new RandomRangeCreator(ctxt, numSetSize, seed++);
			ValueSet values = bind.getType().apply(visitor, expansionLimit);
			result.put(bind.toString(), values);
		}
		
		return new Results(false, result);
	}

	@Override
	public String help()
	{
		return getName() + " [-random:n <size>][-random:s <size>]";
	}

	@Override
	public boolean useByDefault()
	{
		return false;	// Don't use if no -p given
	}
}
