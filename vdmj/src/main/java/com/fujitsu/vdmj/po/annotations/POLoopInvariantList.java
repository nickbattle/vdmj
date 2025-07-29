/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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
import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.expressions.POAndExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.tc.annotations.TCLoopInvariantAnnotation;
import com.fujitsu.vdmj.tc.annotations.TCLoopInvariantList;
import com.fujitsu.vdmj.tc.types.TCBooleanType;

public class POLoopInvariantList extends POMappedList<TCLoopInvariantAnnotation, POLoopInvariantAnnotation>
{
	private final POAssignmentDefinition ghostDef;
	
	public POLoopInvariantList(TCLoopInvariantList from, POAssignmentDefinition ghostDef) throws Exception
	{
		super(from);
		this.ghostDef = ghostDef;
	}

	public POAssignmentDefinition getGhostDef()
	{
		return ghostDef;
	}

	/**
	 * Used to produce an AND expression combining all of the @LoopInvariants passed in, but
	 * possibly excluding those that refer to a loop variables.
	 */
	public POExpression combine(boolean exclLoopVars)
	{
		LexLocation loc = get(0).location;
		LexKeywordToken AND = new LexKeywordToken(Token.AND, loc);
		TCBooleanType BOOL = new TCBooleanType(loc);

		POExpression exp = null;

		for (POLoopInvariantAnnotation loopInv: this)
		{
			if (!exclLoopVars || !loopInv.hasLoopVars)
			{
				if (exp == null)
				{
					exp = loopInv.expression;
				}
				else
				{
					exp = new POAndExpression(exp, AND, loopInv.expression, BOOL, BOOL);
				}
			}
		}

		return exp;
	}
}
