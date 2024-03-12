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

import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.expressions.TCExpression;

import quickcheck.QuickCheck;
import quickcheck.visitors.TrivialQCVisitor;

public class TrivialQCStrategy extends QCStrategy
{
	private int errorCount = 0;

	public TrivialQCStrategy(List<?> argv)
	{
		if (!argv.isEmpty() && argv.get(0) instanceof String)
		{
			for (int i=0; i < argv.size(); i++)
			{
				String arg = (String)argv.get(i);
	
				if (arg.startsWith("-trivial:"))
				{
					println("Unknown trivial option: " + arg);
					println(help());
					errorCount ++;
					argv.remove(i);
				}
			}
		}
		else
		{
			@SuppressWarnings({ "unchecked", "unused" })
			Map<String, Object> map = getParams((List<Map<String, Object>>) argv, "trivial");
		}
	}
	
	@Override
	public String getName()
	{
		return "trivial";
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
		long before = System.currentTimeMillis();

		if (po.isCheckable && po.getCheckedExpression() != null && !po.hasObligations())
		{
			TrivialQCVisitor visitor = new TrivialQCVisitor();

			if (po.getCheckedExpression().apply(visitor, new Stack<TCExpression>()))
			{
				return new StrategyResults(getName(), visitor.getMessage(), null, System.currentTimeMillis() - before);
			}
			
			verbose("No trivial patterns found\n");
		}
		
		return new StrategyResults();	// Got nothing!
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
