/*******************************************************************************
 *
 *	Copyright (c) 2025 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.po.types.visitors;

import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.ast.lex.LexCharacterToken;
import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexQuoteToken;
import com.fujitsu.vdmj.ast.lex.LexRealToken;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.expressions.POBooleanLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POCharLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POIntegerLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POMapEnumExpression;
import com.fujitsu.vdmj.po.expressions.POMapletExpressionList;
import com.fujitsu.vdmj.po.expressions.POMkBasicExpression;
import com.fujitsu.vdmj.po.expressions.POMkTypeExpression;
import com.fujitsu.vdmj.po.expressions.PONilExpression;
import com.fujitsu.vdmj.po.expressions.POQuoteLiteralExpression;
import com.fujitsu.vdmj.po.expressions.PORealLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POSeqEnumExpression;
import com.fujitsu.vdmj.po.expressions.POSetEnumExpression;
import com.fujitsu.vdmj.po.expressions.POTupleExpression;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCInMapType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

/**
 * A visitor to analyse a type and return a POExpression that is a valid default
 * value of that type. For example, a "nat1" type might have an expression of "0"
 * as a POIntegerLiteralExpression.
 */
public class DefaultExpressionCreator extends TCTypeVisitor<POExpression, Object>
{
	private final LexLocation location;

	public DefaultExpressionCreator(LexLocation location)
	{
		this.location = location;
	}

	public DefaultExpressionCreator()
	{
		this.location = LexLocation.ANY;
	}

	@Override
	public POExpression caseType(TCType node, Object arg)
	{
		throw new UnsupportedOperationException("Cannot calculate default for " + node);
	}
	
	@Override
	public POExpression caseBooleanType(TCBooleanType node, Object arg)
	{
		return new POBooleanLiteralExpression(LexBooleanToken.TRUE);
	}

	@Override
	public POExpression caseBracketType(TCBracketType node, Object arg)
	{
		return node.type.apply(this, null);
	}

	@Override
	public POExpression caseCharacterType(TCCharacterType node, Object arg)
	{
		return new POCharLiteralExpression(new LexCharacterToken('a', location));
	}

	@Override
	public POExpression caseInMapType(TCInMapType node, Object arg)
	{
		return new POMapEnumExpression(location,
			new POMapletExpressionList(), new TCTypeList(node.from), new TCTypeList(node.to));
	}

	@Override
	public POExpression caseIntegerType(TCIntegerType node, Object arg)
	{
		try
		{
			return new POIntegerLiteralExpression(new LexIntegerToken("0", location));
		}
		catch (LexException e)
		{
			return null;
		}
	}

	@Override
	public POExpression caseMapType(TCMapType node, Object arg)
	{
		return new POMapEnumExpression(location,
			new POMapletExpressionList(), new TCTypeList(node.from), new TCTypeList(node.to));
	}

	@Override
	public POExpression caseNamedType(TCNamedType node, Object arg)
	{
		return node.type.apply(this, null);
	}

	@Override
	public POExpression caseNaturalOneType(TCNaturalOneType node, Object arg)
	{
		try
		{
			return new POIntegerLiteralExpression(new LexIntegerToken("1", location));
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Override
	public POExpression caseNaturalType(TCNaturalType node, Object arg)
	{
		try
		{
			return new POIntegerLiteralExpression(new LexIntegerToken("0", location));
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Override
	public POExpression caseOptionalType(TCOptionalType node, Object arg)
	{
		return new PONilExpression(location);
	}

	@Override
	public POExpression caseProductType(TCProductType node, Object arg)
	{
		POExpressionList list = new POExpressionList();
		TCTypeList types = new TCTypeList();

		for (TCType type: node.types)
		{
			list.add(type.apply(this, null));
			types.add(type);
		}

		return new POTupleExpression(location, list, types);
	}

	@Override
	public POExpression caseQuoteType(TCQuoteType node, Object arg)
	{
		return new POQuoteLiteralExpression(new LexQuoteToken(node.value, location));
	}

	@Override
	public POExpression caseRationalType(TCRationalType node, Object arg)
	{
		try
		{
			return new POIntegerLiteralExpression(new LexIntegerToken("0", location));
		}
		catch (LexException e)
		{
			return null;
		}
	}

	@Override
	public POExpression caseRealType(TCRealType node, Object arg)
	{
		return new PORealLiteralExpression(new LexRealToken(0.0, location));
	}

	@Override
	public POExpression caseRecordType(TCRecordType node, Object arg)
	{
		POExpressionList values = new POExpressionList();
		TCTypeList types = new TCTypeList();

		for (TCField field: node.fields)
		{
			values.add(field.type.apply(this, null));
			types.add(field.type);
		}

		return new POMkTypeExpression(node.name, values, node, types);
	}

	@Override
	public POExpression caseSeq1Type(TCSeq1Type node, Object arg)
	{
		POExpressionList values = new POExpressionList();
		TCTypeList types = new TCTypeList();
		values.add(node.seqof.apply(this, null));
		types.add(node.seqof);
		return new POSeqEnumExpression(location, values, types);
	}

	@Override
	public POExpression caseSeqType(TCSeqType node, Object arg)
	{
		POExpressionList values = new POExpressionList();
		TCTypeList types = new TCTypeList();
		return new POSeqEnumExpression(location, values, types);
	}

	@Override
	public POExpression caseSet1Type(TCSet1Type node, Object arg)
	{
		POExpressionList values = new POExpressionList();
		TCTypeList types = new TCTypeList();
		values.add(node.setof.apply(this, null));
		types.add(node.setof);
		return new POSetEnumExpression(location, values, types);
	}

	@Override
	public POExpression caseSetType(TCSetType node, Object arg)
	{
		POExpressionList values = new POExpressionList();
		TCTypeList types = new TCTypeList();
		return new POSetEnumExpression(location, values, types);
	}

	@Override
	public POExpression caseTokenType(TCTokenType node, Object arg)
	{
		return new POMkBasicExpression(node, new PONilExpression(location));
	}

	@Override
	public POExpression caseUnionType(TCUnionType node, Object arg)
	{
		return node.types.first().apply(this, null);
	}
}