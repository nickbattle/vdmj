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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCForAllStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCPattern pattern;
	public final TCExpression set;
	public final TCStatement statement;

	public TCType setType;

	public TCForAllStatement(LexLocation location,
		TCPattern pattern, TCExpression set, TCStatement stmt)
	{
		super(location);
		this.pattern = pattern;
		this.set = set;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return "for all " + pattern + " in set " + set + " do\n" + statement;
	}

	@Override
	public TCType typeCheck(Environment base, NameScope scope, TCType constraint, boolean mandatory)
	{
		setType = set.typeCheck(base, null, scope, null);
		pattern.typeResolve(base);

		if (setType.isSet(location))
		{
			TCSetType st = setType.getSet();
			TCDefinitionList defs = pattern.getDefinitions(st.setof, NameScope.LOCAL);

			Environment local = new FlatCheckedEnvironment(defs, base, scope);
			TCType rt = statement.typeCheck(local, scope, constraint, mandatory);
			
			if (!(st instanceof TCSet1Type) &&!(rt instanceof TCVoidType))
			{
				// Union with () because the loop may not be entered
				rt = new TCUnionType(location, rt, new TCVoidType(location));
			}
			
			local.unusedCheck();
			return setType(rt);
		}
		else
		{
			report(3219, "For all statement does not contain a set type");
			return setType(new TCUnknownType(location));
		}
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForAllStatement(this, arg);
	}
}
