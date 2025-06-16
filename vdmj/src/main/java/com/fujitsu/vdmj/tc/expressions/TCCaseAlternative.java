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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.patterns.TCExpressionPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCCaseAlternative extends TCNode
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final TCExpression cexp;
	public final TCPattern pattern;
	public final TCExpression result;

	private TCDefinitionList defs = null;

	public TCCaseAlternative(TCExpression cexp, TCPattern pattern, TCExpression result)
	{
		this.location = pattern.location;
		this.cexp = cexp;
		this.pattern = pattern;
		this.result = result;
	}

	@Override
	public String toString()
	{
		return pattern + " -> " + result;
	}

	public TCType typeCheck(Environment base, NameScope scope, TCType expType, TCType constraint)
	{
		if (defs == null)
		{
			defs = new TCDefinitionList();
			pattern.typeResolve(base);

			if (pattern instanceof TCExpressionPattern)
			{
				// Only expression patterns need type checking...
				TCExpressionPattern ep = (TCExpressionPattern)pattern;
				TCType ptype = ep.exp.typeCheck(base, null, scope, null);
				
				if (!TypeComparator.compatible(ptype, expType))
				{
					pattern.report(3311, "Pattern cannot match");
				}
			}

			try
			{
				pattern.typeResolve(base);
				defs.addAll(pattern.getDefinitions(expType, NameScope.LOCAL));
			}
			catch (TypeCheckException e)
			{
				defs = null;
				throw e;
			}
		}

		defs.typeCheck(base, scope);
		
		if (!pattern.matches(expType))
		{
			pattern.report(3311, "Pattern cannot match");
		}
		
		Environment local = new FlatCheckedEnvironment(defs, base, scope);
		TCType r = result.typeCheck(local, null, scope, constraint);
		local.unusedCheck();
		return r;
	}
}
