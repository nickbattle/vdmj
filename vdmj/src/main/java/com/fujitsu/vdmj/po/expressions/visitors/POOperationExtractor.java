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

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.expressions.POAbsoluteExpression;
import com.fujitsu.vdmj.po.expressions.POAndExpression;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POCardinalityExpression;
import com.fujitsu.vdmj.po.expressions.PODistConcatExpression;
import com.fujitsu.vdmj.po.expressions.PODistIntersectExpression;
import com.fujitsu.vdmj.po.expressions.PODistMergeExpression;
import com.fujitsu.vdmj.po.expressions.PODistUnionExpression;
import com.fujitsu.vdmj.po.expressions.PODivExpression;
import com.fujitsu.vdmj.po.expressions.PODivideExpression;
import com.fujitsu.vdmj.po.expressions.POElementsExpression;
import com.fujitsu.vdmj.po.expressions.POEquivalentExpression;
import com.fujitsu.vdmj.po.expressions.POExists1Expression;
import com.fujitsu.vdmj.po.expressions.POExistsExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POFloorExpression;
import com.fujitsu.vdmj.po.expressions.POForAllExpression;
import com.fujitsu.vdmj.po.expressions.POGreaterEqualExpression;
import com.fujitsu.vdmj.po.expressions.POGreaterExpression;
import com.fujitsu.vdmj.po.expressions.POHeadExpression;
import com.fujitsu.vdmj.po.expressions.POImpliesExpression;
import com.fujitsu.vdmj.po.expressions.POIndicesExpression;
import com.fujitsu.vdmj.po.expressions.POIotaExpression;
import com.fujitsu.vdmj.po.expressions.POLenExpression;
import com.fujitsu.vdmj.po.expressions.POLessEqualExpression;
import com.fujitsu.vdmj.po.expressions.POLessExpression;
import com.fujitsu.vdmj.po.expressions.POMapDomainExpression;
import com.fujitsu.vdmj.po.expressions.POMapInverseExpression;
import com.fujitsu.vdmj.po.expressions.POMapRangeExpression;
import com.fujitsu.vdmj.po.expressions.POModExpression;
import com.fujitsu.vdmj.po.expressions.PONotExpression;
import com.fujitsu.vdmj.po.expressions.POOrExpression;
import com.fujitsu.vdmj.po.expressions.POPlusExpression;
import com.fujitsu.vdmj.po.expressions.POPowerSetExpression;
import com.fujitsu.vdmj.po.expressions.PORemExpression;
import com.fujitsu.vdmj.po.expressions.POReverseExpression;
import com.fujitsu.vdmj.po.expressions.POSeqConcatExpression;
import com.fujitsu.vdmj.po.expressions.POSeqEnumExpression;
import com.fujitsu.vdmj.po.expressions.POSetEnumExpression;
import com.fujitsu.vdmj.po.expressions.POSetUnionExpression;
import com.fujitsu.vdmj.po.expressions.POSubtractExpression;
import com.fujitsu.vdmj.po.expressions.POTailExpression;
import com.fujitsu.vdmj.po.expressions.POTimesExpression;
import com.fujitsu.vdmj.po.expressions.POTupleExpression;
import com.fujitsu.vdmj.po.expressions.POUnaryMinusExpression;
import com.fujitsu.vdmj.po.expressions.POUnaryPlusExpression;
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
	 * so far. We return the unchecked expression, unless the type has POExpression fields,
	 * which indicates a missing visitor method.
	 */
	@Override
	public POExpression caseExpression(POExpression node, Object arg)
	{
		for (Field field: node.getClass().getFields())
		{
			if (field.getType().isAssignableFrom(POExpression.class))
			{
				String message = node.getClass().getSimpleName() + "." + field.getName() + " unchecked";
				throw new POOperationExtractionException(node, message);
			}
		}

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

	@Override
	public POExpression caseIotaExpression(POIotaExpression node, Object arg)
	{
		throw new POOperationExtractionException(node, "iota quantifier");
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

	@Override
	public POExpression caseDivExpression(PODivExpression node, Object arg)
	{
		return setType(node, new PODivExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.DIV, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseRemExpression(PORemExpression node, Object arg)
	{
		return setType(node, new PORemExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.REM, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseModExpression(POModExpression node, Object arg)
	{
		return setType(node, new POModExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.MOD, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseGreaterEqualExpression(POGreaterEqualExpression node, Object arg)
	{
		return setType(node, new POGreaterEqualExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.GE, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseGreaterExpression(POGreaterExpression node, Object arg)
	{
		return setType(node, new POGreaterExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.GT, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseLessEqualExpression(POLessEqualExpression node, Object arg)
	{
		return setType(node, new POLessEqualExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.LE, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseLessExpression(POLessExpression node, Object arg)
	{
		return setType(node, new POLessExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.LT, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseAndExpression(POAndExpression node, Object arg)
	{
		return setType(node, new POAndExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.AND, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseOrExpression(POOrExpression node, Object arg)
	{
		return setType(node, new POOrExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.OR, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseImpliesExpression(POImpliesExpression node, Object arg)
	{
		return setType(node, new POImpliesExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.IMPLIES, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseEquivalentExpression(POEquivalentExpression node, Object arg)
	{
		return setType(node, new POEquivalentExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.IMPLIES, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseNotExpression(PONotExpression node, Object arg)
	{
		return setType(node, new PONotExpression(
			node.location,
			node.exp.apply(this, null)));
	}

	@Override
	public POExpression caseSetUnionExpression(POSetUnionExpression node, Object arg)
	{
		return setType(node, new POSetUnionExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.UNION, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseSeqConcatExpression(POSeqConcatExpression node, Object arg)
	{
		return setType(node, new POSeqConcatExpression(
			node.left.apply(this, null),
			new LexKeywordToken(Token.CONCATENATE, node.location),
			node.right.apply(this, null),
			node.ltype,
			node.rtype));
	}

	@Override
	public POExpression caseSeqEnumExpression(POSeqEnumExpression node, Object arg)
	{
		return setType(node, new POSeqEnumExpression(
			node.location, applyList(node.members, arg), node.types));
	}

	@Override
	public POExpression caseSetEnumExpression(POSetEnumExpression node, Object arg)
	{
		return setType(node, new POSetEnumExpression(
			node.location, applyList(node.members, arg), node.types));
	}

	@Override
	public POExpression caseTupleExpression(POTupleExpression node, Object arg)
	{
		return setType(node, new POTupleExpression(
			node.location, applyList(node.args, arg), node.types));
	}

	@Override
	public POExpression caseCardinalityExpression(POCardinalityExpression node, Object arg)
	{
		return setType(node, new POCardinalityExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseElementsExpression(POElementsExpression node, Object arg)
	{
		return setType(node, new POElementsExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseIndicesExpression(POIndicesExpression node, Object arg)
	{
		return setType(node, new POIndicesExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseHeadExpression(POHeadExpression node, Object arg)
	{
		return setType(node, new POHeadExpression(
			node.location, node.exp.apply(this, arg), node.etype));
	}

	@Override
	public POExpression caseTailExpression(POTailExpression node, Object arg)
	{
		return setType(node, new POTailExpression(
			node.location, node.exp.apply(this, arg), node.etype));
	}

	@Override
	public POExpression caseLenExpression(POLenExpression node, Object arg)
	{
		return setType(node, new POLenExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseAbsoluteExpression(POAbsoluteExpression node, Object arg)
	{
		return setType(node, new POAbsoluteExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseDistConcatExpression(PODistConcatExpression node, Object arg)
	{
		return setType(node, new PODistConcatExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseDistIntersectExpression(PODistIntersectExpression node, Object arg)
	{
		return setType(node, new PODistIntersectExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseDistMergeExpression(PODistMergeExpression node, Object arg)
	{
		return setType(node, new PODistMergeExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseDistUnionExpression(PODistUnionExpression node, Object arg)
	{
		return setType(node, new PODistUnionExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseFloorExpression(POFloorExpression node, Object arg)
	{
		return setType(node, new POFloorExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseMapDomainExpression(POMapDomainExpression node, Object arg)
	{
		return setType(node, new POMapDomainExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseMapRangeExpression(POMapRangeExpression node, Object arg)
	{
		return setType(node, new POMapRangeExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseMapInverseExpression(POMapInverseExpression node, Object arg)
	{
		return setType(node, new POMapInverseExpression(
			node.location, node.exp.apply(this, arg), node.type));
	}

	@Override
	public POExpression casePowerSetExpression(POPowerSetExpression node, Object arg)
	{
		return setType(node, new POPowerSetExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseReverseExpression(POReverseExpression node, Object arg)
	{
		return setType(node, new POReverseExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseUnaryMinusExpression(POUnaryMinusExpression node, Object arg)
	{
		return setType(node, new POUnaryMinusExpression(
			node.location, node.exp.apply(this, arg)));
	}

	@Override
	public POExpression caseUnaryPlusExpression(POUnaryPlusExpression node, Object arg)
	{
		return setType(node, new POUnaryPlusExpression(
			node.location, node.exp.apply(this, arg)));
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
