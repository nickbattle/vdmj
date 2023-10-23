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

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.QuickCheck;
import quickcheck.visitors.TrivialQCVisitor;

public class TrivialQCStrategy extends QCStrategy
{
	private int errorCount = 0;

	public TrivialQCStrategy(List<String> argv)
	{
		for (int i=0; i < argv.size(); i++)
		{
			// No plugin arguments yet?

			if (argv.get(i).startsWith("-trivial:"))
			{
				errorln("Unknown trivial option: " + argv.get(i));
				errorln(help());
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
	public Results getValues(ProofObligation po, INExpression exp, List<INBindingSetter> binds, Context ctxt)
	{
		long before = System.currentTimeMillis();
		String provedBy = null;

		if (po.isCheckable && po.getCheckedExpression() != null)
		{
			TrivialQCVisitor visitor = new TrivialQCVisitor();
			if (po.getCheckedExpression().apply(visitor, new Stack<TCExpression>()))
			{
				provedBy = getName();
			}
		}
		
		return new Results(provedBy, false, new HashMap<String, ValueList>(), System.currentTimeMillis() - before);
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
