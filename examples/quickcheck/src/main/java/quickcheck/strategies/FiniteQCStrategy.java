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
import static com.fujitsu.vdmj.plugins.PluginConsole.verbose;

import java.util.HashMap;
import java.util.List;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.in.types.visitors.INGetAllValuesVisitor;
import com.fujitsu.vdmj.in.types.visitors.INTypeSizeVisitor;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.QuickCheck;

public class FiniteQCStrategy extends QCStrategy
{
	private int expansionLimit = 100000;
	private int errorCount = 0;

	public FiniteQCStrategy(List<String> argv)
	{
		for (int i=0; i < argv.size(); i++)
		{
			try
			{
				switch (argv.get(i))
				{
					case "-finite:size":		// Total top level size = type size
						argv.remove(i);

						if (i < argv.size())
						{
							expansionLimit = Integer.parseInt(argv.get(i));
							argv.remove(i);
						}
						break;

					default:
						if (argv.get(i).startsWith("-finite:"))
						{
							errorln("Unknown finite option: " + argv.get(i));
							errorln(help());
							errorCount++;
							argv.remove(i);
						}
				}
			}
			catch (NumberFormatException e)
			{
				errorln("Argument must be numeric");
				errorln(help());
				errorCount++;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				errorln("Missing argument");
				errorln(help());
				errorCount++;
			}
		}
		
		verbose("finite:size = %d\n", expansionLimit);
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
		return true;
	}

	@Override
	public Results getValues(ProofObligation po, INExpression exp, List<INBindingSetter> binds)
	{
		HashMap<String, ValueList> result = new HashMap<String, ValueList>();
		long before = System.currentTimeMillis();
		boolean proved = false;
		
		if (po.isCheckable)
		{
			Context ctxt = Interpreter.getInstance().getInitialContext();
			long product = 1;
			
			for (INBindingSetter bind: binds)
			{
				try
				{
					long size = bind.getType().apply(new INTypeSizeVisitor(), ctxt).longValue();
					product = product * size;	// cumulative for each bind
					
			   		if (product > expansionLimit)
					{
			   			return new Results(false, result, 0);	// Too big
					}
				}
				catch (InternalException e)		// Infinite
				{
					return new Results(false, result, 0);
				}
				catch (ArithmeticException e)	// Overflow probably
				{
					return new Results(false, result, 0);
				}
			}
			
			// Game on...
			for (INBindingSetter bind: binds)
			{
				result.put(bind.toString(), bind.getType().apply(new INGetAllValuesVisitor(), ctxt));
			}
			
			proved = true;	// ie. if the counterexamples above pass, then PROVED
		}
		
		return new Results(proved, result, System.currentTimeMillis() - before);
	}

	@Override
	public String help()
	{
		return getName() + " [-finite:size <size>]";
	}
}
