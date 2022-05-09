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
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;

public class CasesExhaustiveObligation extends ProofObligation
{
	public CasesExhaustiveObligation(POCasesExpression exp, POContextStack ctxt)
	{
		super(exp.location, POType.CASES_EXHAUSTIVE, ctxt);
		StringBuilder sb = new StringBuilder();
		String prefix = "";

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
				POPattern noIgnores = alt.pattern.removeIgnorePatterns();
				POExpression matching = noIgnores.getMatchingExpression();
				
	    		sb.append("(exists ");
	    		sb.append(noIgnores);
	    		sb.append(":");
	    		sb.append(explicitType(exp.expType, exp.location));
	    		sb.append(" & ");
	    		sb.append(exp.exp);
	    		sb.append(" = ");
	    		sb.append(matching);
	    		sb.append(")");
			}

			prefix = " or ";
		}

		value = ctxt.getObligation(sb.toString());
	}
}
