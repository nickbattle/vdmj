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

package com.fujitsu.vdmj.tc.statements;

import java.util.Iterator;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCVoidReturnType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

abstract public class TCSimpleBlockStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCStatementList statements;

	public TCSimpleBlockStatement(LexLocation location, TCStatementList statements)
	{
		super(location);
		this.statements = statements;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		String sep = "";

		for (TCStatement s: statements)
		{
			sb.append(sep);
			sb.append(s.toString());
			sep = ";\n";
		}

		sb.append("\n");
		return sb.toString();
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		boolean notreached = false;
		TCTypeSet rtypes = new TCTypeSet();
		TCType last = null;
		Iterator<TCStatement> iter = statements.iterator(); 

		while (iter.hasNext())
		{
			TCStatement stmt = iter.next();
			TCType stype = stmt.typeCheck(env, scope, constraint, mandatory && !iter.hasNext());

			if (notreached)
			{
				stmt.warning(5006, "Statement will not be reached");
			}
			else
			{
				last = stype;
				notreached = true;

    			if (stype instanceof TCUnionType)
    			{
    				TCUnionType ust = (TCUnionType)stype;

    				for (TCType t: ust.types)
    				{
    					addOne(rtypes, t);

    					if (t instanceof TCVoidType ||
    						t instanceof TCUnknownType)
    					{
    						notreached = false;
    					}
    				}
    			}
    			else
    			{
    				addOne(rtypes, stype);

					if (stype instanceof TCVoidType ||
						stype instanceof TCUnknownType)
					{
						notreached = false;
					}
    			}
			}
		}

		// If the last statement reached has a void component, add this to the overall
		// return type, as the block may return nothing.

		if (last != null &&
			(last.isType(TCVoidType.class, location) ||	last.isUnknown(location)))
		{
			rtypes.add(new TCVoidType(location));
		}

		return setType(rtypes.isEmpty() ?
			new TCVoidType(location) : rtypes.getType(location));
	}

	private void addOne(TCTypeSet rtypes, TCType add)
	{
		if (add instanceof TCVoidReturnType)
		{
			rtypes.add(new TCVoidType(add.location));
		}
		else if (!(add instanceof TCVoidType))
		{
			rtypes.add(add);
		}
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSimpleBlockStatement(this, arg);
	}
}
