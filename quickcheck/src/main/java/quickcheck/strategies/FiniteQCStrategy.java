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
import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.in.types.visitors.INGetAllValuesVisitor;
import com.fujitsu.vdmj.in.types.visitors.INTypeSizeVisitor;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.QuickCheck;

public class FiniteQCStrategy extends QCStrategy
{
	private int expansionLimit = 1024;	// Small and fast?
	private int errorCount = 0;

	public FiniteQCStrategy(List<String> argv)
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			try
			{
				String arg = iter.next();
				
				switch (arg)
				{
					case "-finite:size":		// Total top level size = type size
						iter.remove();

						if (iter.hasNext())
						{
							expansionLimit = Integer.parseInt(iter.next());
							iter.remove();
						}
						break;

					default:
						if (arg.startsWith("-finite:"))
						{
							println("Unknown finite option: " + arg);
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
		return "finite";
	}

	@Override
	public boolean hasErrors()
	{
		return errorCount > 0;
	}

	@Override
	public boolean useByDefault()
	{
		return true;
	}

	@Override
	public boolean init(QuickCheck qc)
	{
		verbose("finite:size = %d\n", expansionLimit);
		return true;
	}

	@Override
	public StrategyResults getValues(ProofObligation po, INExpression exp, List<INBindingOverride> binds, Context ctxt)
	{
		HashMap<String, ValueList> result = new HashMap<String, ValueList>();
		long before = System.currentTimeMillis();
		
		if (po.isCheckable)
		{
			long product = 1;
			
			for (INBindingOverride bind: binds)
			{
				try
				{
					long size = bind.getType().apply(new INTypeSizeVisitor(), ctxt).longValue();
					product = product * size;	// cumulative for each bind
					verbose("Size of %s type is %s\n", bind, product);
					
			   		if (product > expansionLimit)
					{
			   			verbose("Size is greater than %d limit\n", expansionLimit);
			   			return new StrategyResults();	// Too big
					}
				}
				catch (InternalException e)		// Infinite
				{
		   			verbose("Size of bind %s is greater than %d limit\n", bind, expansionLimit);
					return new StrategyResults();
				}
				catch (ArithmeticException e)	// Overflow probably
				{
		   			verbose("Size of bind %s is greater than %d limit\n", bind, expansionLimit);
					return new StrategyResults();
				}
			}
			
			// Game on... all binds can be expanded
			for (INBindingOverride bind: binds)
			{
				result.put(bind.toString(), bind.getType().apply(new INGetAllValuesVisitor(), ctxt));
			}
		}
		
		// We can only claim to have all values if there are no parameter types.
		// The current bind may be finite, but others may not be.
		boolean hasAll = (po.typeParams == null);
		
		return new StrategyResults(result, hasAll, System.currentTimeMillis() - before);
	}

	@Override
	public String help()
	{
		return getName() + " [-finite:size <size>]";
	}
}
