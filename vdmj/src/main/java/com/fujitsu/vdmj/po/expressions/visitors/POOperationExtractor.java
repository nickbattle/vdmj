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

package com.fujitsu.vdmj.po.expressions.visitors;

import java.util.LinkedHashMap;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.PODivideExpression;
import com.fujitsu.vdmj.po.expressions.POExists1Expression;
import com.fujitsu.vdmj.po.expressions.POExistsExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POForAllExpression;
import com.fujitsu.vdmj.po.expressions.POPlusExpression;
import com.fujitsu.vdmj.po.expressions.POSubtractExpression;
import com.fujitsu.vdmj.po.expressions.POTimesExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * Visitor to explore an expression and edit out all of the operation calls,
 * replacing them with variable expressions and creating a table of variables
 * and their operation calls. This is used by the POG to generate contexts
 * for expressions that contain operation calls.
 * 
 * Failing expressions throw a POOperationExtractionException.
 */
public class POOperationExtractor extends POExpressionVisitor<POExpression, Object>
{
	private final LinkedHashMap<TCNameToken, POApplyExpression> substitutions;

	public POOperationExtractor()
	{
		this.substitutions = new LinkedHashMap<TCNameToken, POApplyExpression>();
	}

	public LinkedHashMap<TCNameToken, POApplyExpression> getSubstitutions()
	{
		return substitutions;
	}

	/**
	 * The base case catches all of the expression types that have not been implemented
	 * so far. We just return the unchecked expression, even though some parts may have
	 * operation calls?
	 */
	@Override
	public POExpression caseExpression(POExpression node, Object arg)
	{
		return node;
	}

	/**
	 * Apply expressions are substituted if they relate to operation calls.
	 */
	@Override
	public POExpression caseApplyExpression(POApplyExpression node, Object arg)
	{
		if (node.type.isOperation(node.location))
		{
			return substitute(node);
		}
		else
		{
			return setType(node, new POApplyExpression(
				node.root.apply(this, arg),
				applyList(node.args, arg),
				node.type,
				node.argtypes,
				node.recursiveCycles,
				node.opdef));
		}
	}

	/**
	 * The following expressions cannot be substituted, because they have an unknown
	 * number of operation calls.
	 */
	@Override
	public POExpression caseForAllExpression(POForAllExpression node, Object arg)
	{
		throw new POOperationExtractionException(node, "forall quantifier");
	}

	@Override
	public POExpression caseExistsExpression(POExistsExpression node, Object arg)
	{
		throw new POOperationExtractionException(node, "exists quantifier");
	}

	@Override
	public POExpression caseExists1Expression(POExists1Expression node, Object arg)
	{
		throw new POOperationExtractionException(node, "exists1 quantifier");
	}

	/**
	 * Implemented substitutions. Each case returns a copy of itself, with any sub-expressions
	 * processed by the same visitor.
	 */

	@Override
	public POExpression casePlusExpression(POPlusExpression node, Object arg)
	{
		return setType(node, new POPlusExpression(
			node.left.apply(this, arg),
			new LexKeywordToken(Token.PLUS, node.location),
			node.right.apply(this, arg),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseSubtractExpression(POSubtractExpression node, Object arg)
	{
		return setType(node, new POSubtractExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.MINUS, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseTimesExpression(POTimesExpression node, Object arg)
	{
		return setType(node, new POTimesExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.TIMES, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseDivideExpression(PODivideExpression node, Object arg)
	{
		return setType(node, new PODivideExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.DIVIDE, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	/**
	 * Add a substitution for this operation call, by creating a name, adding it to the
	 * table, and returning a POVariableExpression of the substitute name.
	 */
	private POExpression substitute(POApplyExpression node)
	{
		TCNameToken name = null;

		if (node.root instanceof POVariableExpression)
		{
			POVariableExpression root = (POVariableExpression)node.root;

			name = new TCNameToken(node.location, root.name.getModule(), "$" + root.name.getName());
			int count = 1;

			while (substitutions.containsKey(name))
			{
				name = new TCNameToken(node.location, root.name.getModule(), "$" + root.name.getName() + "_" + count);
				count++;
			}

			substitutions.put(name, node);		// eg. "$op1" -> op1(a,b,c)

			return setType(node, new POVariableExpression(name, null));
		}
		else
		{
			throw new POOperationExtractionException(node, "no operation name?");
		}
	}

	/**
	 * Apply the visitor to a list of expressions.
	 */
	private POExpressionList applyList(POExpressionList arglist, Object arg)
	{
		POExpressionList result = new POExpressionList();

		for (POExpression exp: arglist)
		{
			result.add(exp.apply(this, null));
		}

		return result;
	}

	/**
	 * Copy the exptype into a new object.
	 */
	private POExpression setType(POExpression node, POExpression exp)
	{
		exp.setExptype(node.getExptype());
		return exp;
	}
}
