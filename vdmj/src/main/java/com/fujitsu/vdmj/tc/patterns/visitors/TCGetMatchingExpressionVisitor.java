/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package com.fujitsu.vdmj.tc.patterns.visitors;

import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.expressions.TCBooleanLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCCharLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapUnionExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapletExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapletExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
import com.fujitsu.vdmj.tc.expressions.TCNewExpression;
import com.fujitsu.vdmj.tc.expressions.TCNilExpression;
import com.fujitsu.vdmj.tc.expressions.TCQuoteLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCRealLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqConcatExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetUnionExpression;
import com.fujitsu.vdmj.tc.expressions.TCStringLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCTupleExpression;
import com.fujitsu.vdmj.tc.expressions.TCUndefinedExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.patterns.TCBooleanPattern;
import com.fujitsu.vdmj.tc.patterns.TCCharacterPattern;
import com.fujitsu.vdmj.tc.patterns.TCConcatenationPattern;
import com.fujitsu.vdmj.tc.patterns.TCExpressionPattern;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCIgnorePattern;
import com.fujitsu.vdmj.tc.patterns.TCIntegerPattern;
import com.fujitsu.vdmj.tc.patterns.TCMapPattern;
import com.fujitsu.vdmj.tc.patterns.TCMapUnionPattern;
import com.fujitsu.vdmj.tc.patterns.TCMapletPattern;
import com.fujitsu.vdmj.tc.patterns.TCNamePatternPair;
import com.fujitsu.vdmj.tc.patterns.TCNilPattern;
import com.fujitsu.vdmj.tc.patterns.TCObjectPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCQuotePattern;
import com.fujitsu.vdmj.tc.patterns.TCRealPattern;
import com.fujitsu.vdmj.tc.patterns.TCRecordPattern;
import com.fujitsu.vdmj.tc.patterns.TCSeqPattern;
import com.fujitsu.vdmj.tc.patterns.TCSetPattern;
import com.fujitsu.vdmj.tc.patterns.TCStringPattern;
import com.fujitsu.vdmj.tc.patterns.TCTuplePattern;
import com.fujitsu.vdmj.tc.patterns.TCUnionPattern;

public class TCGetMatchingExpressionVisitor extends TCPatternVisitor<TCExpression, Object>
{
	@Override
	public TCExpression casePattern(TCPattern node, Object arg)
	{
		throw new RuntimeException("Missing POGetMatchingExpressionVisitor method!");
	}
	
	@Override
	public TCExpression caseBooleanPattern(TCBooleanPattern node, Object arg)
	{
		return new TCBooleanLiteralExpression(node.value);
	}
	
	@Override
	public TCExpression caseCharacterPattern(TCCharacterPattern node, Object arg)
	{
		return new TCCharLiteralExpression(node.value);
	}
	
	@Override
	public TCExpression caseConcatenationPattern(TCConcatenationPattern node, Object arg)
	{
		LexToken op = new LexKeywordToken(Token.CONCATENATE, node.location);
		return new TCSeqConcatExpression(
			node.left.apply(this, arg), op, node.right.apply(this, arg));
	}
	
	@Override
	public TCExpression caseExpressionPattern(TCExpressionPattern node, Object arg)
	{
		return node.exp;
	}
	
	@Override
	public TCExpression caseIdentifierPattern(TCIdentifierPattern node, Object arg)
	{
		return new TCVariableExpression(node.location, node.name, node.name.getName());
	}
	
	@Override
	public TCExpression caseIgnorePattern(TCIgnorePattern node, Object arg)
	{
		return new TCUndefinedExpression(node.location);
	}
	
	@Override
	public TCExpression caseIntegerPattern(TCIntegerPattern node, Object arg)
	{
		return new TCIntegerLiteralExpression(node.value);
	}
	
	@Override
	public TCExpression caseMapPattern(TCMapPattern node, Object arg)
	{
		TCMapletExpressionList list = new TCMapletExpressionList();

		for (TCMapletPattern p: node.maplets)
		{
			list.add(new TCMapletExpression(p.from.apply(this, arg), p.to.apply(this, arg)));
		}

		return new TCMapEnumExpression(node.location, list);
	}
	
	@Override
	public TCExpression caseMapUnionPattern(TCMapUnionPattern node, Object arg)
	{
		LexToken op = new LexKeywordToken(Token.MUNION, node.location);
		return new TCMapUnionExpression(
			node.left.apply(this, arg), op, node.right.apply(this, arg));
	}
	
	@Override
	public TCExpression caseNilPattern(TCNilPattern node, Object arg)
	{
		return new TCNilExpression(node.location);
	}
	
	@Override
	public TCExpression caseObjectPattern(TCObjectPattern node, Object arg)
	{
		TCExpressionList list = new TCExpressionList();

		for (TCNamePatternPair npp: node.fieldlist)
		{
			list.add(npp.pattern.apply(this, arg));
		}

		// Note... this may not actually match obj_C(...)
		return new TCNewExpression(node.location,
			new TCIdentifierToken(node.classname.getLocation(), node.classname.getName(), false), list);
	}
	
	@Override
	public TCExpression caseQuotePattern(TCQuotePattern node, Object arg)
	{
		return new TCQuoteLiteralExpression(node.location, node.value);
	}
	
	@Override
	public TCExpression caseRealPattern(TCRealPattern node, Object arg)
	{
		return new TCRealLiteralExpression(node.value);
	}
	
	@Override
	public TCExpression caseRecordPattern(TCRecordPattern node, Object arg)
	{
		TCExpressionList list = new TCExpressionList();

		for (TCPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new TCMkTypeExpression(node.typename, list, false);
	}
	
	@Override
	public TCExpression caseSeqPattern(TCSeqPattern node, Object arg)
	{
		TCExpressionList list = new TCExpressionList();

		for (TCPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new TCSeqEnumExpression(node.location, list);
	}
	
	@Override
	public TCExpression caseSetPattern(TCSetPattern node, Object arg)
	{
		TCExpressionList list = new TCExpressionList();

		for (TCPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new TCSetEnumExpression(node.location, list);
	}
	
	@Override
	public TCExpression caseStringPattern(TCStringPattern node, Object arg)
	{
		return new TCStringLiteralExpression(node.value);
	}
	
	@Override
	public TCExpression caseTuplePattern(TCTuplePattern node, Object arg)
	{
		TCExpressionList list = new TCExpressionList();

		for (TCPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return new TCTupleExpression(node.location, list);
	}
	
	@Override
	public TCExpression caseUnionPattern(TCUnionPattern node, Object arg)
	{
		LexToken op = new LexKeywordToken(Token.UNION, node.location);
		return new TCSetUnionExpression(
			node.left.apply(this, arg), op, node.right.apply(this, arg));
	}
}
