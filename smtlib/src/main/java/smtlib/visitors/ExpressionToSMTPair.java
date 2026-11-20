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

import com.fujitsu.vdmj.tc.expressions.TCBinaryExpression;
import com.fujitsu.vdmj.tc.expressions.TCBooleanLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCHeadExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.typechecker.Environment;

import smtlib.ast.Expression;
import smtlib.ast.GT;
import smtlib.ast.Text;

public class ExpressionToSMTPair extends TCExpressionVisitor<SMTPair, Environment>
{
	@Override
	public SMTPair caseExpression(TCExpression node, Environment env)
	{
		throw new UnsupportedOperationException("Unsupported Pair type: " + node.getClass().getSimpleName());
	}

	@Override
	public SMTPair caseIntegerLiteralExpression(TCIntegerLiteralExpression node, Environment env)
	{
		return new SMTPair(new Expression(Long.toString(node.value.value)));
	}

	@Override
	public SMTPair caseBooleanLiteralExpression(TCBooleanLiteralExpression node, Environment env)
	{
		return new SMTPair(new Expression(Boolean.toString(node.value.value)));
	}

	@Override
	public SMTPair caseSeqEnumExpression(TCSeqEnumExpression node, Environment env)
	{
		if (node.members.isEmpty())
		{
			QualifiedSort qsort = node.getType().apply(new TypeToSMT("?"), env);
			return new SMTPair(new Expression("as", new Text("seq.empty"), qsort.sort));
		}
		else if (node.members.size() == 1)
		{
			SMTPair first = node.members.firstElement().apply(this, env);
			return new SMTPair(new Expression("seq.unit", first.value), first.delta);
		}
		else
		{
			Expression result = new Expression("seq.++");
			SMTPairList pairs = new SMTPairList();

			for (TCExpression e: node.members)
			{
				SMTPair pair = e.apply(this, env);
				result.add(new Expression("seq.unit", pair.value));
				pairs.add(pair);
			}

			return new SMTPair(result, pairs.deltas());
		}
	}

	@Override
	public SMTPair caseHeadExpression(TCHeadExpression node, Environment env)
	{
		SMTPair seq = node.exp.apply(this, env);

		return new SMTPair(
			new Expression(
				new Text("seq.nth"),
				seq.value,
				new Text("0")),
			SMTPair.deltas(
					seq.delta,
					new GT(
						new Expression("seq.len", seq.value),
						new Text("0"))));
	}

	@Override
	public SMTPair caseBinaryExpression(TCBinaryExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new Expression(node.op.toString(), left.value, right.value),
			SMTPair.deltas(left, right));
	}
}
