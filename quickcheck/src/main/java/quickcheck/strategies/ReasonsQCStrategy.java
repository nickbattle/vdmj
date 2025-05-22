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

import java.util.List;

import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVariableFinder;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A class to hold the MAYBE "reasons about" heuristic.
 */
public class ReasonsQCStrategy extends QCStrategy
{
	public ReasonsQCStrategy(List<String> argv)
	{
		for (int i=0; i < argv.size(); i++)
		{
			String arg = argv.get(i);
			
			if (arg.startsWith("-reasons:"))
			{
				println("Unknown reasons option: " + argv);
				println(help());
				errorCount ++;
				argv.remove(i);
			}
		}
	}

	@Override
	public String getName()
	{
		return "reasons";
	}
	
	@Override
	public void maybeHeuristic(ProofObligation po)
	{
		boolean isfunc =
			(po.definition instanceof POExplicitFunctionDefinition ||
			po.definition instanceof POImplicitFunctionDefinition);
		
		if (isfunc && po.obligationVars != null && po.reasonsAbout != null)
		{
			if (po.reasonsAbout.contains(POExpressionVariableFinder.SOMETHING))
			{
				po.setMessage(null);	// Something => nothing missing
			}
			else
			{
				TCNameSet missing = new TCNameSet();
				missing.addAll(po.obligationVars);
				missing.removeAll(po.reasonsAbout);
				
				if (!missing.isEmpty())
				{
					StringBuilder sb = new StringBuilder("Note: does not check ");
					String sep = "";
					
					for (TCNameToken var: missing)
					{
						sb.append(sep);
						sb.append(var);
						sep = ", ";
					}
					
					sb.append("?");
					po.setMessage(sb.toString());
				}
			}
		}
	}
}
