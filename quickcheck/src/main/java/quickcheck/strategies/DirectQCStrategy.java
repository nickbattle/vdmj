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
import static com.fujitsu.vdmj.plugins.PluginConsole.verboseln;

import java.util.List;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.pog.NonZeroObligation;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.TotalFunctionObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNumericType;

import quickcheck.QuickCheck;
import quickcheck.visitors.TotalExpressionVisitor;

/**
 * A QC strategy to look for particular obligation types, to discharge them by
 * some form of direct analysis, rather than looking at the obligation exp.
 */
public class DirectQCStrategy extends QCStrategy
{
	private int errorCount = 0;
	
	public DirectQCStrategy(List<String> argv)
	{
		for (int i=0; i < argv.size(); i++)
		{
			if (argv.get(i).startsWith("-direct:"))
			{
				println("Unknown direct option: " + argv.get(i));
				println(help());
				errorCount ++;
				argv.remove(i);
			}
		}
	}

	@Override
	public String getName()
	{
		return "direct";
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
	public StrategyResults getValues(ProofObligation po, INExpression exp, List<INBindingOverride> binds, Context ctxt)
	{
		if (po.hasObligations())
		{
			verboseln("Obligation with POs cannot be proved directly");
			return new StrategyResults();
		}
		
		switch (po.kind)
		{
			case TOTAL_FUNCTION:
				verboseln("Trying direct proof of total obligation");
				return directTotalObligation((TotalFunctionObligation) po);
			
			case NON_ZERO:
				verboseln("Trying direct proof of non-zero obligation");
				return directNonZeroObligation((NonZeroObligation) po);
				
			default:
				verboseln("Obligation cannot be proved directly");
				return new StrategyResults();
		}
	}

	private StrategyResults directNonZeroObligation(NonZeroObligation po)
	{
		long before = System.currentTimeMillis();
		TCNumericType ntype = po.right.getExptype().getNumeric();
		
		if (ntype instanceof TCNaturalOneType)
		{
			return new StrategyResults(getName(), "(nat1 cannot be zero)",
					null, System.currentTimeMillis() - before);
		}
		
		return new StrategyResults();
	}

	private StrategyResults directTotalObligation(TotalFunctionObligation po)
	{
		TotalExpressionVisitor visitor = new TotalExpressionVisitor();
		POExplicitFunctionDefinition exdef = (POExplicitFunctionDefinition) po.definition;
		
		long before = System.currentTimeMillis();
		exdef.body.apply(visitor, null);
		
		if (visitor.isTotal())
		{
			return new StrategyResults(getName(), "(body is total)", null, System.currentTimeMillis() - before);
		}
		else
		{
			return new StrategyResults();
		}
	}

	@Override
	public String help()
	{
		return getName() + " (no options)";
	}

}
