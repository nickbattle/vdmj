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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.in.types.visitors.INTypeSizeVisitor;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.expressions.POCaseAlternative;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.pog.CasesExhaustiveObligation;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.TotalFunctionObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.typechecker.TypeComparator;

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
		switch (po.kind)
		{
			case TOTAL_FUNCTION:
				verboseln("Trying direct proof of total obligation");
				return directTotalObligation((TotalFunctionObligation) po);
			
			case CASES_EXHAUSTIVE:
				verboseln("Trying direct proof of exhaustive cases obligation");
				return directCasesObligation((CasesExhaustiveObligation) po);
				
			default:
				verboseln("Obligation cannot be proved directly");
				return new StrategyResults();
		}
	}

	private StrategyResults directCasesObligation(CasesExhaustiveObligation po)
	{
		try
		{
			long before = System.currentTimeMillis();
			Context ctxt = Interpreter.getInstance().getInitialContext();
			long typeSize = po.exp.expType.apply(new INTypeSizeVisitor(), ctxt);
			
			if (typeSize > po.exp.cases.size())
			{
				return new StrategyResults();	// Impossible
			}
			
			Set<POPattern> unique = new HashSet<POPattern>();
			
			for (POCaseAlternative p: po.exp.cases)
			{
				if (p.pattern.isSimple())
				{
					unique.add(p.pattern);
				}
			}
			
			if (unique.size() == typeSize)
			{
				return new StrategyResults(getName(), "(patterns match type values)", null, System.currentTimeMillis() - before);
			}
		}
		catch (InternalException e)
		{
			// Infinite subtype encountered...
		}
		
		return new StrategyResults();	// Complex patterns, so we're not sure
	}

	private StrategyResults directTotalObligation(TotalFunctionObligation po)
	{
		TotalExpressionVisitor visitor = new TotalExpressionVisitor();
		POExplicitFunctionDefinition exdef = (POExplicitFunctionDefinition) po.definition;
		
		if (exdef.bodyObligationCount > 0)
		{
			return new StrategyResults();	// Body can fail in principle, so not definitely total
		}

		if (exdef.isUndefined ||
			!TypeComparator.isSubType(exdef.actualResult, exdef.expectedResult))
		{
			return new StrategyResults();	// Body may not be the right type, so not definitely total
		}

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
