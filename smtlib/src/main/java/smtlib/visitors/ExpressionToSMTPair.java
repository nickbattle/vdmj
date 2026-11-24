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

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCBinaryExpression;
import com.fujitsu.vdmj.tc.expressions.TCBooleanLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.expressions.TCHeadExpression;
import com.fujitsu.vdmj.tc.expressions.TCIfExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Pair;

import smtlib.ast.And;
import smtlib.ast.Bracketed;
import smtlib.ast.Exists;
import smtlib.ast.Expression;
import smtlib.ast.ExpressionSet;
import smtlib.ast.ForAll;
import smtlib.ast.GT;
import smtlib.ast.ITE;
import smtlib.ast.Implies;
import smtlib.ast.Text;

public class ExpressionToSMTPair extends TCExpressionVisitor<SMTPair, Environment> implements SMTHelper
{
	@Override
	public SMTPair caseExpression(TCExpression node, Environment env)
	{
		throw new UnsupportedOperationException("Unsupported Pair type: " + node.getClass().getSimpleName());
	}

	@Override
	public SMTPair caseVariableExpression(TCVariableExpression node, Environment arg)
	{
		return new SMTPair(new Expression(node.name.toString()));
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
			ExpressionSet deltas = new ExpressionSet();

			for (TCExpression e: node.members)
			{
				SMTPair pair = e.apply(this, env);
				result.add(new Expression("seq.unit", pair.value));
				deltas.addAll(pair.delta);
			}

			return new SMTPair(result, deltas);
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
			new ExpressionSet(
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
			new ExpressionSet(left.delta, right.delta));
	}

	@Override
	public SMTPair caseIfExpression(TCIfExpression node, Environment env)
	{
		SMTPair ifExp = node.ifExp.apply(this, env);
		SMTPair thenExp = node.thenExp.apply(this, env);
		SMTPair elseExp = node.elseExp.apply(this, env);

		// TODO elseif list

		ITE value = new ITE(ifExp.value, thenExp.value, elseExp.value);
		ExpressionSet delta = new ExpressionSet();

		if (ifExp.delta != null)
		{
			delta.addAll(ifExp.delta);
		}

		if (!thenExp.delta.isEmpty() || !elseExp.delta.isEmpty())
		{
			delta.add(new ITE(ifExp.value,
				thenExp.delta.andOf(),
				elseExp.delta.andOf()));
		}

		return new SMTPair(value, delta);
	}

	@Override
	public SMTPair caseForAllExpression(TCForAllExpression node, Environment env)
	{
		Pair<Bracketed, Expression> pair = expandQuantifier(true, node.bindList, node.predicate, env);
		return new SMTPair(new ForAll(pair.first, pair.second));
	}

	@Override
	public SMTPair caseExistsExpression(TCExistsExpression node, Environment env)
	{
		Pair<Bracketed, Expression> pair = expandQuantifier(false, node.bindList, node.predicate, env);
		return new SMTPair(new Exists(pair.first, pair.second));
	}

	private Pair<Bracketed, Expression> expandQuantifier(
		boolean forall, TCMultipleBindList bindList, TCExpression predicate, Environment env)
	{
		ExpressionSet qualifiers = new ExpressionSet();
		TCExpressionList letquals = new TCExpressionList();
		Bracketed newBindList = new Bracketed();
		
		for (TCMultipleBind bind: bindList)
		{
			if (bind instanceof TCMultipleTypeBind)
			{
				TCMultipleTypeBind mtbind = (TCMultipleTypeBind)bind;
				TCType updatedType = mtbind.type;
				TCPatternList updatedPlist = new TCPatternList();
				updatedPlist.addAll(mtbind.plist);

				for (int i=0; i < mtbind.plist.size(); i++)
				{
					TCIdentifierPattern p2 = Utils.checkPattern(mtbind.plist.get(i), mtbind.type, letquals, env);
					updatedPlist.set(i, p2);
					
					TCDefinition def = p2.getDefinitions(mtbind.type, NameScope.LOCAL).firstElement();
					TCNameToken var = def.name;

					QualifiedType qtype = def.getType().apply(new TypeExpander(var), env);
					QualifiedSort qsort = def.getType().apply(new TypeToSMT(var), env);
					updatedType = qtype.type;

					if (qsort.qualifier != null)
					{
						qualifiers.add(qsort.qualifier);
					}
				}

				for (TCPattern pattern: updatedPlist)
				{
					for (TCDefinition pdef: pattern.getDefinitions(updatedType, NameScope.LOCAL))
					{
						QualifiedSort qsort = pdef.getType().apply(new TypeToSMT("?"), env);
						newBindList.add(new Expression(pdef.name.getName(), qsort.sort));
					}
				}

			}
			// else if (bind instanceof TCMultipleSeqBind)
			// {
			// 	TCMultipleSeqBind msbind = (TCMultipleSeqBind)bind;
			// 	TCSeqType stype = msbind.sequence.getType().getSeq();
			// 	TCType updatedType = stype.seqof;
			// 	TCPatternList updatedPlist = new TCPatternList();
			// 	updatedPlist.addAll(msbind.plist);
			// 	stype.apply(new TypeExpander(), env);		// Add any type defs needed

			// 	for (int i=0; i < msbind.plist.size(); i++)
			// 	{
			// 		TCIdentifierPattern p2 = Utils.checkPattern(msbind.plist.get(i), stype.seqof, letquals, env);
			// 		updatedPlist.set(i, p2);

			// 		TCDefinition def = p2.getDefinitions(stype.seqof, NameScope.LOCAL).firstElement();
			// 		TCNameToken var = def.name;
			// 		QualifiedType qtype = def.getType().apply(new TypeExpander(var), env);
			// 		updatedType = qtype.type;

			// 		if (qtype.qualifier != null)
			// 		{
			// 			qualifiers.add(qtype.qualifier);
			// 		}

			// 		// And p2 in set elems S
			// 		TCVariableExpression pvar = new TCVariableExpression(p2.location, p2.name, p2.toString());
			// 		TCElementsExpression elems = new TCElementsExpression(p2.location, msbind.sequence.apply(this, bodyConstraints));
			// 		qualifiers.add(new TCInSetExpression(pvar, INSET, elems));
			// 	}

			// 	// Turn x in seq S into x:T, given qualifier above
			// 	newBindList.add(new TCMultipleTypeBind(updatedPlist, updatedType));
			// }
			// else if (bind instanceof TCMultipleSetBind)
			// {
			// 	TCMultipleSetBind msbind = (TCMultipleSetBind)bind;
			// 	TCSetType stype = msbind.set.getType().getSet();
			// 	TCType updatedType = stype.setof;
			// 	TCPatternList updatedPlist = new TCPatternList();
			// 	updatedPlist.addAll(msbind.plist);
			// 	stype.apply(new TypeExpander(), env);		// Add any type defs needed
				
			// 	for (int i=0; i < msbind.plist.size(); i++)
			// 	{
			// 		TCIdentifierPattern p2 = Utils.checkPattern(msbind.plist.get(i), stype.setof, letquals, env);
			// 		updatedPlist.set(i, p2);

			// 		TCDefinition def = p2.getDefinitions(stype.setof, NameScope.LOCAL).firstElement();
			// 		TCNameToken var = def.name;
			// 		QualifiedType qtype = def.getType().apply(new TypeExpander(var), env);
			// 		updatedType = qtype.type;

			// 		if (qtype.qualifier != null)
			// 		{
			// 			qualifiers.add(qtype.qualifier);
			// 		}

			// 		// And p2 in set S
			// 		TCVariableExpression pvar = new TCVariableExpression(p2.location, p2.name, p2.toString());
			// 		qualifiers.add(new TCInSetExpression(pvar, INSET, msbind.set.apply(this, bodyConstraints)));
			// 	}

			// 	// Turn x in set S into x:T, given qualifier above
			// 	newBindList.add(new TCMultipleTypeBind(updatedPlist, updatedType));
			// }
			else
			{
				throw new UnsupportedOperationException("Cannot expand: " + bind);
			}
		}

		SMTPair body = predicate.apply(this, env);

		// [<qualifiers> (=>/and) [<bodyConstraints> and]] <translated body>

		if (!body.delta.isEmpty())
		{
			body = new SMTPair(new And(body.delta.andOf(), body.value));
		}

		if (!qualifiers.isEmpty())
		{
			if (forall)
			{
				body = new SMTPair(new Implies(qualifiers.andOf(), body.value));
			}
			else
			{
				body = new SMTPair(new And(qualifiers.andOf(), body.value));
			}
		}

		if (!letquals.isEmpty())
		{
			body = Utils.combineQualifiers(letquals, body.value);
		}

		return new Pair<Bracketed, Expression>(newBindList, body.value);
	}
}
