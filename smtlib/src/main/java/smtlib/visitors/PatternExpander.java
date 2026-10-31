/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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

package smtlib.visitors;

import java.util.Map.Entry;
import java.util.TreeMap;

import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCBooleanLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCCharLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCEqualsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCExpressionSet;
import com.fujitsu.vdmj.tc.expressions.TCFieldExpression;
import com.fujitsu.vdmj.tc.expressions.TCFieldNumberExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCQuoteLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCRealLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCStringLiteralExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.patterns.TCBooleanPattern;
import com.fujitsu.vdmj.tc.patterns.TCCharacterPattern;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCIgnorePattern;
import com.fujitsu.vdmj.tc.patterns.TCIntegerPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCQuotePattern;
import com.fujitsu.vdmj.tc.patterns.TCRealPattern;
import com.fujitsu.vdmj.tc.patterns.TCRecordPattern;
import com.fujitsu.vdmj.tc.patterns.TCSeqPattern;
import com.fujitsu.vdmj.tc.patterns.TCStringPattern;
import com.fujitsu.vdmj.tc.patterns.TCTuplePattern;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.patterns.visitors.TCPatternVisitor;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeqType;

public class PatternExpander extends TCPatternVisitor<Object, TCExpression> implements ASTHelper
{
	private final TreeMap<TCTypeBind, TCExpressionSet> bindings;
	private final TCExpressionSet constraints;

	public PatternExpander()
	{
		this.bindings = new TreeMap<TCTypeBind, TCExpressionSet>();
		this.constraints = new TCExpressionSet();
	}

	@Override
	public Object casePattern(TCPattern node, TCExpression arg)
	{
		throw new UnsupportedOperationException("Cannot expand pattern: " + node);
	}

	@Override
	public Object caseIdentifierPattern(TCIdentifierPattern node, TCExpression arg)
	{
		if (!node.toString().equals(arg.toString()))	// ie. not "let x=x in ..."
		{
			TCTypeBind bind = new TCTypeBind(node, arg.getType());
			TCExpressionSet set = bindings.get(bind);

			if (set == null)
			{
				set = new TCExpressionSet();
				bindings.put(bind, set);
			}

			set.add(arg);
		}

		return null;
	}

	@Override
	public Object caseIgnorePattern(TCIgnorePattern node, TCExpression arg)
	{
		return null;	// Ignored!
	}

	/**
	 * Fixed patterns add constraints, such that the arg expression equals the pattern constant.
	 */
	@Override
	public Object caseIntegerPattern(TCIntegerPattern node, TCExpression arg)
	{
		constraints.add(new TCEqualsExpression(arg, EQ, new TCIntegerLiteralExpression(node.value)));
		return null;
	}

	@Override
	public Object caseRealPattern(TCRealPattern node, TCExpression arg)
	{
		constraints.add(new TCEqualsExpression(arg, EQ, new TCRealLiteralExpression(node.value)));
		return null;
	}

	@Override
	public Object caseBooleanPattern(TCBooleanPattern node, TCExpression arg)
	{
		constraints.add(new TCEqualsExpression(arg, EQ, new TCBooleanLiteralExpression(node.value)));
		return null;
	}

	@Override
	public Object caseCharacterPattern(TCCharacterPattern node, TCExpression arg)
	{
		constraints.add(new TCEqualsExpression(arg, EQ, new TCCharLiteralExpression(node.value)));
		return null;
	}

	@Override
	public Object caseQuotePattern(TCQuotePattern node, TCExpression arg)
	{
		constraints.add(new TCEqualsExpression(arg, EQ, new TCQuoteLiteralExpression(ANYWHERE, node.value)));
		return null;
	}

	@Override
	public Object caseStringPattern(TCStringPattern node, TCExpression arg)
	{
		constraints.add(new TCEqualsExpression(arg, EQ, new TCStringLiteralExpression(node.value)));
		return null;
	}
	
	@Override
	public Object caseRecordPattern(TCRecordPattern node, TCExpression arg)
	{
		TCRecordType rec = (TCRecordType)node.type;
		int fnum = 0;

		for (TCField field: rec.fields)
		{
			TCIdentifierToken tag = new TCIdentifierToken(LexLocation.ANY, field.tag, false);
			TCExpression subexp = new TCFieldExpression(arg, tag, null);
			subexp.setType(field.type);
			node.plist.get(fnum).apply(this, subexp);
			fnum++;
		}

		return null;
	}

	@Override
	public Object caseTuplePattern(TCTuplePattern node, TCExpression arg)
	{
		TCProductType product = arg.getType().getProduct();

		for (int i=0; i < node.plist.size(); i++)
		{
			LexIntegerToken fld = new LexIntegerToken(Long.valueOf(i + 1), ANYWHERE);
			TCExpression subexp = new TCFieldNumberExpression(arg, fld);
			subexp.setType(product.types.get(i));
			node.plist.get(i).apply(this, subexp);
		}

		return null;
	}

	@Override
	public Object caseSeqPattern(TCSeqPattern node, TCExpression arg)
	{
		int indx = 1;

		for (TCPattern p: node.plist)
		{
			TCExpressionList args = new TCExpressionList(
				new TCIntegerLiteralExpression(new LexIntegerToken(indx, ANYWHERE)));

			TCExpression subexp = new TCApplyExpression(arg, args);
			TCSeqType stype = arg.getType().getSeq();
			subexp.setType(stype.seqof);
			p.apply(this, subexp);
			indx++;
		}

		return null;
	}

	/**
	 * Get the results of the pattern analysis...
	 */

	public TreeMap<TCTypeBind, TCExpression> getBindings()
	{
		TreeMap<TCTypeBind, TCExpression> result = new TreeMap<TCTypeBind, TCExpression>();

		for (Entry<TCTypeBind, TCExpressionSet> entry: bindings.entrySet())
		{
			result.put(entry.getKey(), Utils.andOf(entry.getValue()));
		}

		return result;
	}
}
