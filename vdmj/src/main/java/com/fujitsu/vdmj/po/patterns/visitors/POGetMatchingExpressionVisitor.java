/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj.po.patterns.visitors;

import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.expressions.POBooleanLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POCharLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POIntegerLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POMapEnumExpression;
import com.fujitsu.vdmj.po.expressions.POMapUnionExpression;
import com.fujitsu.vdmj.po.expressions.POMapletExpression;
import com.fujitsu.vdmj.po.expressions.POMapletExpressionList;
import com.fujitsu.vdmj.po.expressions.POMkTypeExpression;
import com.fujitsu.vdmj.po.expressions.PONewExpression;
import com.fujitsu.vdmj.po.expressions.PONilExpression;
import com.fujitsu.vdmj.po.expressions.POQuoteLiteralExpression;
import com.fujitsu.vdmj.po.expressions.PORealLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POSeqConcatExpression;
import com.fujitsu.vdmj.po.expressions.POSeqEnumExpression;
import com.fujitsu.vdmj.po.expressions.POSetEnumExpression;
import com.fujitsu.vdmj.po.expressions.POSetUnionExpression;
import com.fujitsu.vdmj.po.expressions.POStringLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POTupleExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.patterns.POBooleanPattern;
import com.fujitsu.vdmj.po.patterns.POCharacterPattern;
import com.fujitsu.vdmj.po.patterns.POConcatenationPattern;
import com.fujitsu.vdmj.po.patterns.POExpressionPattern;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.patterns.POIgnorePattern;
import com.fujitsu.vdmj.po.patterns.POIntegerPattern;
import com.fujitsu.vdmj.po.patterns.POMapPattern;
import com.fujitsu.vdmj.po.patterns.POMapUnionPattern;
import com.fujitsu.vdmj.po.patterns.POMapletPattern;
import com.fujitsu.vdmj.po.patterns.PONamePatternPair;
import com.fujitsu.vdmj.po.patterns.PONilPattern;
import com.fujitsu.vdmj.po.patterns.POObjectPattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POQuotePattern;
import com.fujitsu.vdmj.po.patterns.PORealPattern;
import com.fujitsu.vdmj.po.patterns.PORecordPattern;
import com.fujitsu.vdmj.po.patterns.POSeqPattern;
import com.fujitsu.vdmj.po.patterns.POSetPattern;
import com.fujitsu.vdmj.po.patterns.POStringPattern;
import com.fujitsu.vdmj.po.patterns.POTuplePattern;
import com.fujitsu.vdmj.po.patterns.POUnionPattern;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class POGetMatchingExpressionVisitor extends POPatternVisitor<POExpression, Object>
{
	private static int var = 0;		// Used in caseIgnorePattern()

	public static void init()
	{
		var = 0;	// reset on each getProofObligations run.
	}

	@Override
	public POExpression casePattern(POPattern node, Object arg)
	{
		throw new RuntimeException("Missing POGetMatchingExpressionVisitor method!");
	}
	
	@Override
	public POExpression caseBooleanPattern(POBooleanPattern node, Object arg)
	{
		return new POBooleanLiteralExpression(node.value);
	}
	
	@Override
	public POExpression caseCharacterPattern(POCharacterPattern node, Object arg)
	{
		return new POCharLiteralExpression(node.value);
	}
	
	@Override
	public POExpression caseConcatenationPattern(POConcatenationPattern node, Object arg)
	{
		LexToken op = new LexKeywordToken(Token.CONCATENATE, node.location);
		return new POSeqConcatExpression(
			node.left.apply(this, arg), op, node.right.apply(this, arg), null, null);
	}
	
	@Override
	public POExpression caseExpressionPattern(POExpressionPattern node, Object arg)
	{
		return node.exp;
	}
	
	@Override
	public POExpression caseIdentifierPattern(POIdentifierPattern node, Object arg)
	{
		return new POVariableExpression(node.name, null);
	}
	
	@Override
	public POExpression caseIgnorePattern(POIgnorePattern node, Object arg)
	{
		// Generate a new "any" name for use during PO generation. The name
		// must be unique for the pattern instance.
		
		var++;
		TCNameToken anyName = new TCNameToken(node.location, node.location.module, "$any" + var);
		
		return new POVariableExpression(anyName, null);
	}
	
	@Override
	public POExpression caseIntegerPattern(POIntegerPattern node, Object arg)
	{
		return new POIntegerLiteralExpression(node.value);
	}
	
	@Override
	public POExpression caseMapPattern(POMapPattern node, Object arg)
	{
		POMapletExpressionList list = new POMapletExpressionList();

		for (POMapletPattern p: node.maplets)
		{
			list.add(new POMapletExpression(p.location, p.from.apply(this, arg), p.to.apply(this, arg)));
		}

		return new POMapEnumExpression(node.location, list, null, null);
	}
	
	@Override
	public POExpression caseMapUnionPattern(POMapUnionPattern node, Object arg)
	{
		LexToken op = new LexKeywordToken(Token.MUNION, node.location);
		return new POMapUnionExpression(
			node.left.apply(this, arg), op, node.right.apply(this, arg), null, null);
	}
	
	@Override
	public POExpression caseNilPattern(PONilPattern node, Object arg)
	{
		return new PONilExpression(node.location);
	}
	
	@Override
	public POExpression caseObjectPattern(POObjectPattern node, Object arg)
	{
		POExpressionList list = new POExpressionList();

		for (PONamePatternPair npp: node.fieldlist)
		{
			list.add(npp.pattern.apply(this, arg));
		}

		// Note... this may not actually match obj_C(...)
		return new PONewExpression(node.location,
			new TCIdentifierToken(node.classname.getLocation(), node.classname.getName(), false), list);
	}
	
	@Override
	public POExpression caseQuotePattern(POQuotePattern node, Object arg)
	{
		return new POQuoteLiteralExpression(node.value);
	}
	
	@Override
	public POExpression caseRealPattern(PORealPattern node, Object arg)
	{
		return new PORealLiteralExpression(node.value);
	}
	
	@Override
	public POExpression caseRecordPattern(PORecordPattern node, Object arg)
	{
		POExpressionList list = new POExpressionList();

		for (POPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new POMkTypeExpression(node.typename, list, null, null);
	}
	
	@Override
	public POExpression caseSeqPattern(POSeqPattern node, Object arg)
	{
		POExpressionList list = new POExpressionList();

		for (POPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new POSeqEnumExpression(node.location, list, null);
	}
	
	@Override
	public POExpression caseSetPattern(POSetPattern node, Object arg)
	{
		POExpressionList list = new POExpressionList();

		for (POPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new POSetEnumExpression(node.location, list, null);
	}
	
	@Override
	public POExpression caseStringPattern(POStringPattern node, Object arg)
	{
		return new POStringLiteralExpression(node.value);
	}
	
	@Override
	public POExpression caseTuplePattern(POTuplePattern node, Object arg)
	{
		POExpressionList list = new POExpressionList();

		for (POPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new POTupleExpression(node.location, list, null);
	}
	
	@Override
	public POExpression caseUnionPattern(POUnionPattern node, Object arg)
	{
		LexToken op = new LexKeywordToken(Token.UNION, node.location);
		return new POSetUnionExpression(
			node.left.apply(this, arg), op, node.right.apply(this, arg), null, null);
	}
}
