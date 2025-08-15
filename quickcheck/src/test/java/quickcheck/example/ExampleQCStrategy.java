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

package quickcheck.example;

import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.QuickCheck;
import quickcheck.strategies.QCStrategy;
import quickcheck.strategies.StrategyResults;
import quickcheck.strategies.StrategyUpdater;

public class ExampleQCStrategy extends QCStrategy
{
	private boolean provedResult = false;

	public ExampleQCStrategy(List<String> argv)
	{
		// Remove your "qc" plugin arguments from the list here
		// You must include the strategy name, like "-example:n"
		
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
		return "example";	// Can be used with -s <name>
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
		
		for (INBindingOverride bind: binds)
		{
			values.put(bind.toString(), new ValueList());	// ie. nothing, for every bind
		}
		
		if (provedResult)
		{
			return new StrategyResults(new ExampleUpdater("Just an example"));
		}
		else
		{
			return new StrategyResults(values, false);
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

	/**
	 * An updater can be returned in a StrategyResults object, which is then used
	 * to make arbitrary updates to the PO's state.
	 */
	private class ExampleUpdater extends StrategyUpdater
	{
		private final String qualifier;

		public ExampleUpdater(String qualifier)
		{
			this.qualifier = qualifier;
		}

		@Override
		public void updateProofObligation(ProofObligation po)
		{
			// In general, this can do anything to a PO...
			po.setStatus(POStatus.PROVABLE);
			po.setProvedBy(getName());
			po.setQualifier(qualifier);
		}
	}

}
