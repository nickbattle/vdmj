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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.visitors.PORemoveIgnoresVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;

public class PONotCaseContext extends POContext
{
	public final POPattern pattern;
	public final TCType type;
	public final POExpression exp;

	public PONotCaseContext(POPattern pattern, TCType type, POExpression exp)
	{
		this.pattern = pattern;
		this.type = type;
		this.exp = exp;
	}
	
	@Override
	public TCNameSet reasonsAbout()
	{
		TCNameSet result = new TCNameSet();
		result.addAll(pattern.getVariableNames());
		result.addAll(exp.getVariableNames());
		return result;
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		if (pattern.isSimple())
		{
			sb.append("not ");
    		sb.append(pattern);
    		sb.append(" = ");
    		sb.append(exp);
		}
		else
		{
			PORemoveIgnoresVisitor.init();
    		sb.append("not (exists ");
    		sb.append(pattern.removeIgnorePatterns());
    		sb.append(":");
    		sb.append(type.toExplicitString(pattern.location));
    		sb.append(" & ");
    		PORemoveIgnoresVisitor.init();
    		sb.append(pattern.removeIgnorePatterns());
    		sb.append(" = ");
    		sb.append(exp);
    		sb.append(")");
		}

		sb.append(" =>");

		return sb.toString();
	}
}
