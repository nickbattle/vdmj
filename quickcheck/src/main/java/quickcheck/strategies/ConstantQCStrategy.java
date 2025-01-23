/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.visitors.ConstantExpressionFinder;

/**
 * A strategy to look for constants in obligations to create bindings.
 */
public class ConstantQCStrategy extends QCStrategy
{
	public ConstantQCStrategy(List<String> argv)
	{
		for (int i=0; i < argv.size(); i++)
		{
			String arg = argv.get(i);
			
			if (arg.startsWith("-constant:"))
			{
				println("Unknown constant option: " + argv);
				println(help());
				errorCount ++;
				argv.remove(i);
			}
		}
	}

	@Override
	public String getName()
	{
		return "constant";
	}

	@Override
	public StrategyResults getValues(ProofObligation po, List<INBindingOverride> binds, Context ctxt)
	{
		ValueList constants = new ValueList();
		TCTypeList tctypes = new TCTypeList();
		
		try
		{
			TCExpressionList tcconstants = new TCExpressionList();
			po.getCheckedExpression().apply(new ConstantExpressionFinder(), tcconstants);

			for (TCExpression tcexp: tcconstants)
			{
				INExpression inexp = ClassMapper.getInstance(INNode.MAPPINGS).convertLocal(tcexp);
				constants.add(inexp.eval(ctxt));
				tctypes.add(tcexp.getType());
			}
		}
		catch (Exception e)
		{
			verbose("Constant strategy failed: " + e.getMessage());
			return new StrategyResults();
		}
			
		Map<String, ValueList> results = new HashMap<String, ValueList>();
		
		for (INBindingOverride bind: binds)
		{
			TCType bindType = bind.getType();
			String key = bind.toString();
			
			for (int i = 0; i < constants.size(); i++)
			{
				if (TypeComparator.compatible(bindType, tctypes.get(i)))
				{
					if (results.containsKey(key))
					{
						ValueList values = results.get(key);
						values.add(constants.get(i));
					}
					else
					{
						results.put(key, new ValueList(constants.get(i)));
					}
				}
			}
		}
		
		return new StrategyResults(results, false);
	}
	
	@Override
	public boolean useByDefault()
	{
		return false;	// For now
	}
}
