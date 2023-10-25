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

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.visitors.PORemoveIgnoresVisitor;
import com.fujitsu.vdmj.tc.types.TCType;

public class POCaseContext extends POContext
{
	public final POPattern pattern;
	public final TCType type;
	public final POExpression exp;

	public POCaseContext(POPattern pattern, TCType type, POExpression exp)
	{
		this.pattern = pattern;
		this.type = type;
		this.exp = exp;
	}

	@Override
	public String getContext()
	{
		StringBuilder sb = new StringBuilder();

		if (pattern.isSimple())
		{
    		sb.append(pattern);
    		sb.append(" = ");
    		sb.append(exp);
    		sb.append(" => ");
		}
		else
		{
			PORemoveIgnoresVisitor.init();
    		sb.append("exists ");
    		sb.append(pattern.removeIgnorePatterns());
    		sb.append(":");
    		sb.append(type.toExplicitString(pattern.location));
    		sb.append(" & ");
    		PORemoveIgnoresVisitor.init();
			sb.append(pattern.removeIgnorePatterns());
    		sb.append(" = ");
    		sb.append(exp);

    		sb.append(" =>\nlet ");
    		sb.append(pattern);
    		sb.append(" = ");
    		sb.append(exp);
    		sb.append(" in");
		}

		return sb.toString();
	}
}
