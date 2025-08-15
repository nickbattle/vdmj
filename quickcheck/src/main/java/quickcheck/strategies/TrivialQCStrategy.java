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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package quickcheck.strategies;

import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static quickcheck.commands.QCConsole.verbose;

import java.util.List;

import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;

import quickcheck.visitors.TrivialQCVisitor;

public class TrivialQCStrategy extends QCStrategy
{
	public TrivialQCStrategy(List<String> argv)
	{
		for (int i=0; i < argv.size(); i++)
		{
			String arg = argv.get(i);

			if (arg.startsWith("-trivial:"))
			{
				println("Unknown trivial option: " + arg);
				println(help());
				errorCount ++;
				argv.remove(i);
			}
		}
	}
	
	@Override
	public String getName()
	{
		return "trivial";
	}

	@Override
	public StrategyResults getValues(ProofObligation po, List<INBindingOverride> binds, Context ctxt)
	{
		if (po.isCheckable && po.getCheckedExpression() != null && !po.hasObligations())
		{
			TrivialQCVisitor visitor = new TrivialQCVisitor();

			if (po.getCheckedExpression().apply(visitor, new TrivialQCKnown()))
			{
				return new StrategyResults(new TrivialUpdater(visitor.getMessage()));
			}
			
			verbose("No trivial patterns found\n");
		}
		
		return new StrategyResults();	// Got nothing!
	}

	private class TrivialUpdater extends StrategyUpdater
	{
		private final String qualifier;

		public TrivialUpdater(String qualifier)
		{
			this.qualifier = qualifier;
		}

		@Override
		public void updateProofObligation(ProofObligation po)
		{
			po.setStatus(POStatus.PROVABLE);
			po.setProvedBy(getName());
			po.setQualifier(qualifier);
		}
	}
}
