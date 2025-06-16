/*******************************************************************************
 *
 *	Copyright (c) 2008, 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTimeExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;

	public TCTimeExpression(LexLocation location)
	{
		super(location);
	}

	@Override
	public String toString()
	{
		return "time";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCDefinition encl = env.getEnclosingDefinition();
		
		if (encl != null && encl.isPure())
		{
			report(3346, "Cannot use 'time' in pure operations");
		}
		
		if (Settings.release == Release.VDM_10 && env.isFunctional())
		{
			if (env.isFunctionalError())
			{
				report(3348, "Cannot use 'time' in a functional context");
			}
			else
			{
				warning(5036, "Cannot use 'time' in a functional context");
			}
		}

		return checkConstraint(constraint, new TCNaturalType(location));
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTimeExpression(this, arg);
	}
}
