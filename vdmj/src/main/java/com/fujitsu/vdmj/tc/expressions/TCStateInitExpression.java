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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCStateInitExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCStateDefinition state;

	public TCStateInitExpression(TCStateDefinition state)
	{
		super(state.location);
		this.state = state;
	}
	@Override
	public String toString()
	{
		return "init " + state.initPattern + " == " + state.initExpression;
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCPattern pattern = state.initPattern;
		TCExpression exp = state.initExpression;
		boolean canBeExecuted = false;

		if (pattern instanceof TCIdentifierPattern &&
			exp instanceof TCEqualsExpression)
		{
			TCEqualsExpression ee = (TCEqualsExpression)exp;
			ee.left.typeCheck(env, null, scope, null);

			if (ee.left instanceof TCVariableExpression)
			{
				TCType rhs = ee.right.typeCheck(env, null, scope, null);

				if (rhs.isTag())
				{
					TCRecordType rt = rhs.getRecord();
					canBeExecuted = rt.name.equals(state.name);
				}
			}
		}
		else
		{
			exp.typeCheck(env, null, scope, null);
		}

		if (!canBeExecuted)
		{
			exp.warning(5010, "State init expression cannot be executed");
			detail("Expected", "p == p = mk_" + state.name.getName() + "(...)");
		}

		state.canBeExecuted = canBeExecuted;
		return setType(new TCBooleanType(location));
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseStateInitExpression(this, arg);
	}
}
