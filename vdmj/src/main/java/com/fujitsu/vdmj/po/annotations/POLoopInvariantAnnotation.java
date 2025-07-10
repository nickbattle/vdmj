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

package com.fujitsu.vdmj.po.annotations;

import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.expressions.POAndExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;

public class POLoopInvariantAnnotation extends POAnnotation
{
	private static final long serialVersionUID = 1L;
	
	public final POExpression invariant;

	public POLoopInvariantAnnotation(TCIdentifierToken name, POExpressionList args)
	{
		super(name, args);
		this.invariant = args.firstElement();
	}

	/**
	 * Used to produce an AND expression combining all of the @LoopInvariants passed in, but
	 * possibly excluding those that refer to a particular loop variable.
	 */
	public static POExpression combine(POLoopInvariantList invariants, TCNameList excludes)
	{
		LexLocation loc = invariants.get(0).location;
		LexKeywordToken AND = new LexKeywordToken(Token.AND, loc);
		TCBooleanType BOOL = new TCBooleanType(loc);

		POExpression exp = null;

		for (POLoopInvariantAnnotation loopInv: invariants)
		{
			boolean add = true;

			if (excludes != null)
			{
				for (TCNameToken exclude: excludes)
				{
					if (loopInv.invariant.getVariableNames().contains(exclude))
					{
						add = false;	// Contains an excluded name
						break;
					}
				}
			}

			if (add)
			{
				if (exp == null)
				{
					exp = loopInv.invariant;
				}
				else
				{
					exp = new POAndExpression(exp, AND, loopInv.invariant, BOOL, BOOL);
				}
			}
		}

		return exp;
	}
}
