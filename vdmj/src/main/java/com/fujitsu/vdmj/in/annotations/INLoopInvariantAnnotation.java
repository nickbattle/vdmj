/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package com.fujitsu.vdmj.in.annotations;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

public class INLoopInvariantAnnotation extends INAnnotation
{
	private static final long serialVersionUID = 1L;
	private final boolean hasLoopVars;

	public INLoopInvariantAnnotation(TCIdentifierToken name, INExpressionList args, boolean hasLoopVars)
	{
		super(name, args);
		this.hasLoopVars = hasLoopVars;
		setBreaks(false);	// Break on checks instead
	}

	// NOTE: inBefore/inAfter are not used. The check method is called directly by
	// the various loop INStatements via their INLoopAnnotations.
	
	public void check(Context ctxt, boolean inside) throws ValueException
	{
		if (inside || !hasLoopVars)
		{
			INExpression inv = args.get(0);

			if (!inv.eval(ctxt).boolValue(ctxt))
			{
				throw new ValueException(4178, "Loop invariant violated: " + inv, ctxt);
			}
		}
	}
}
