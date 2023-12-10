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

import static quickcheck.commands.QCConsole.println;
import static quickcheck.commands.QCConsole.verbose;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;

import quickcheck.QuickCheck;
import quickcheck.visitors.FixedRangeCreator;
import quickcheck.visitors.RandomRangeCreator;

public class RandomQCStrategy extends QCStrategy
{
	private int expansionLimit = 20;	// Overall returned value limit
	private long seed;
	private int errorCount = 0;

	public RandomQCStrategy(List<String> argv)
	{
		seed = System.currentTimeMillis();
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			try
			{
				String arg = iter.next();
				
				switch (arg)
				{
					case "-random:size":		// Total top level size
						iter.remove();

						if (iter.hasNext())
						{
							expansionLimit = Integer.parseInt(iter.next());
							iter.remove();
						}
						break;
						
					case "-random:seed":		// Seed
						iter.remove();

						if (iter.hasNext())
						{
							seed = Long.parseLong(iter.next());
							iter.remove();
						}
						break;

					default:
						if (arg.startsWith("-random:"))
						{
							println("Unknown random option: " + arg);
							println(help());
							errorCount++;
							iter.remove();
						}
				}
			}
			catch (NumberFormatException e)
			{
				println("Argument must be numeric");
				println(help());
				errorCount++;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				println("Missing argument");
				println(help());
				errorCount++;
			}
		}
	}
	
	@Override
	public String getName()
	{
		return "random";
	}

	@Override
	public boolean hasErrors()
	{
		return errorCount  > 0;
	}

	@Override
	public boolean init(QuickCheck qc)
	{
		verbose("random:size = %d\n", expansionLimit);
		verbose("random:seed = %d\n", seed);
		return true;
	}

	@Override
	public StrategyResults getValues(ProofObligation po, INExpression exp, List<INBindingSetter> binds, Context ctxt)
	{
		HashMap<String, ValueList> result = new HashMap<String, ValueList>();
		long before = System.currentTimeMillis();
		
		if (po.isCheckable && po.getCheckedExpression() != null)
		{
			for (INBindingSetter bind: binds)
			{
				ValueList list = new ValueList();
				
				if (po.hasCorrelatedBinds())	// Must used fixed values by type
				{
					FixedRangeCreator visitor = new FixedRangeCreator(ctxt);
					ValueSet extras = bind.getType().apply(visitor, expansionLimit);
					list.addAll(extras);
				}
				else
				{
					RandomRangeCreator visitor = new RandomRangeCreator(ctxt, seed++);
					ValueSet values = bind.getType().apply(visitor, expansionLimit);
					list.addAll(values);
				}
				
				result.put(bind.toString(), list);
			}
		}
		
		return new StrategyResults(result, false, System.currentTimeMillis() - before);
	}

	@Override
	public String help()
	{
		return getName() + " [-random:size <size>][-random:seed <seed>]";
	}

	@Override
	public boolean useByDefault()
	{
		return true;	// Don't use if no -s given
	}
}
