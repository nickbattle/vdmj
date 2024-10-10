/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.po.expressions.POCaseAlternative;
import com.fujitsu.vdmj.po.expressions.POCasesExpression;
import com.fujitsu.vdmj.po.patterns.visitors.PORemoveIgnoresVisitor;

public class CasesExhaustiveObligation extends ProofObligation
{
	private final boolean hasCorrelatedBinds;
	public final POCasesExpression exp;
	
	public CasesExhaustiveObligation(POCasesExpression exp, POContextStack ctxt)
	{
		super(exp.location, POType.CASES_EXHAUSTIVE, ctxt);
		this.exp = exp;
		
		StringBuilder sb = new StringBuilder();
		String prefix = "";
		boolean correlated = false;

		for (POCaseAlternative alt: exp.cases)
		{
			sb.append(prefix);

			if (alt.pattern.isSimple())
			{
				sb.append(exp.exp);
				sb.append(" = ");
				sb.append(alt.pattern);
			}
			else
			{
	    		PORemoveIgnoresVisitor.init();
	    		sb.append("(exists ");
	    		sb.append(alt.pattern.removeIgnorePatterns());
	    		sb.append(":");
	    		sb.append(explicitType(exp.expType, exp.location));
	    		sb.append(" & ");
	    		PORemoveIgnoresVisitor.init();
	    		sb.append(exp.exp);
	    		sb.append(" = ");
	    		sb.append(alt.pattern.removeIgnorePatterns());
	    		sb.append(")");
	    		
	    		correlated = true;
			}

			prefix = " or ";
		}

		hasCorrelatedBinds = correlated;
		source = ctxt.getObligation(sb.toString());
	}

	@Override
	public boolean hasCorrelatedBinds()
	{
		return hasCorrelatedBinds;
	}
}
