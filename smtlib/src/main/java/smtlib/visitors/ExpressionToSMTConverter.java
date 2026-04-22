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

import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCAbsoluteExpression;
import com.fujitsu.vdmj.tc.expressions.TCAndExpression;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCBooleanLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCCardinalityExpression;
import com.fujitsu.vdmj.tc.expressions.TCCaseAlternative;
import com.fujitsu.vdmj.tc.expressions.TCCasesExpression;
import com.fujitsu.vdmj.tc.expressions.TCDivExpression;
import com.fujitsu.vdmj.tc.expressions.TCDivideExpression;
import com.fujitsu.vdmj.tc.expressions.TCElementsExpression;
import com.fujitsu.vdmj.tc.expressions.TCElseIfExpression;
import com.fujitsu.vdmj.tc.expressions.TCEqualsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCFloorExpression;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterExpression;
import com.fujitsu.vdmj.tc.expressions.TCIfExpression;
import com.fujitsu.vdmj.tc.expressions.TCImpliesExpression;
import com.fujitsu.vdmj.tc.expressions.TCInSetExpression;
import com.fujitsu.vdmj.tc.expressions.TCIndicesExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsExpression;
import com.fujitsu.vdmj.tc.expressions.TCLenExpression;
import com.fujitsu.vdmj.tc.expressions.TCLessEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCLessExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapDomainExpression;
import com.fujitsu.vdmj.tc.expressions.TCModExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotInSetExpression;
import com.fujitsu.vdmj.tc.expressions.TCOrExpression;
import com.fujitsu.vdmj.tc.expressions.TCPlusExpression;
import com.fujitsu.vdmj.tc.expressions.TCProperSubsetExpression;
import com.fujitsu.vdmj.tc.expressions.TCRealLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCRemExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqConcatExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetDifferenceExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetIntersectExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetUnionExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubseqExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubsetExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubtractExpression;
import com.fujitsu.vdmj.tc.expressions.TCTimesExpression;
import com.fujitsu.vdmj.tc.expressions.TCUnaryMinusExpression;
import com.fujitsu.vdmj.tc.expressions.TCUnaryPlusExpression;
import com.fujitsu.vdmj.tc.expressions.TCUndefinedExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCBooleanPattern;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCIntegerPattern;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCRealPattern;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

import smtlib.ast.And;
import smtlib.ast.Bracketed;
import smtlib.ast.EQ;
import smtlib.ast.Exists;
import smtlib.ast.Expression;
import smtlib.ast.ForAll;
import smtlib.ast.GE;
import smtlib.ast.GT;
import smtlib.ast.Implies;
import smtlib.ast.LE;
import smtlib.ast.LT;
import smtlib.ast.Let;
import smtlib.ast.Not;
import smtlib.ast.Or;
import smtlib.ast.Source;
import smtlib.ast.Text;

public class ExpressionToSMTConverter extends TCExpressionVisitor<Expression, Environment>
{
	@Override
	public Expression caseApplyExpression(TCApplyExpression node, Environment env)
	{
		TCDefinition root = findApplyDefinition(node, env);		// Must be s(i)

		if (root instanceof TCExplicitFunctionDefinition)
		{
			TCExplicitFunctionDefinition def = (TCExplicitFunctionDefinition)root;

			if (def.name.getName().startsWith("post_"))
			{
				TCNameToken base = def.name.getBaseName(def.location);
				TCDefinition basedef = env.findName(base, NameScope.GLOBAL);

				if (basedef instanceof TCExplicitFunctionDefinition)
				{
					TCExplicitFunctionDefinition func = (TCExplicitFunctionDefinition)basedef;

					return new Let(
						new Bracketed(new Expression("RESULT", func.body.apply(this, env))),
						def.body.apply(this, env)
					);
				}
			}
			else if (matchingArguments(def, node))	// Includes pre_ cases
			{
				return new Expression(def.body.apply(this, env));
			}
			else	// expand arguments
			{
				Expression call = new Expression(def.name.getName());

				for (TCExpression arg: node.args)
				{
					call.add(arg.apply(this, env));
				}

				return call;
			}
		}
		else if (root.getType().isSeq(root.location))
		{
			TCVariableExpression var = (TCVariableExpression)node.root;
			Expression index = node.args.get(0).apply(this, env);

			// (seq.nth s (- i 1)
			return new Expression(new Text("seq.nth"),
				new Text(var.name.getName()),
				new Expression(new Text("-"), index, new Text("1")));
		}
		else if (root.getType().isMap(root.location))
		{
			TCVariableExpression map = (TCVariableExpression)node.root;
			Expression key = node.args.get(0).apply(this, env);
			String mapname = map.name.getName();

			return new Expression(new Text("select"), new Text(mapname), key);
		}

		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	@Override
	public Expression caseForAllExpression(TCForAllExpression node, Environment env)
	{
		Bracketed binds = new Bracketed();
		Expression body = node.predicate.apply(this, env);
		List<Expression> qualifiers = new Vector<Expression>();
		
		for (TCMultipleBind bind: node.bindList)
		{
			if (bind instanceof TCMultipleTypeBind)
			{
				TCMultipleTypeBind mtbind = (TCMultipleTypeBind)bind;

				for (TCPattern pattern: mtbind.plist)
				{
					for (TCDefinition def: pattern.getDefinitions(mtbind.type, NameScope.LOCAL))
					{
						QualifiedSort qsort = def.getType().apply(new TypeToSMTConverter(def.name.getName()), env);
						binds.add(new Expression(def.name.getName(), qsort.sort));

						if (qsort.qualifier != null)
						{
							qualifiers.add(qsort.qualifier);
						}
					}
				}
			}
			else
			{
				throw new UnsupportedOperationException("Cannot convert: " + bind);
			}
		}

		if (!qualifiers.isEmpty())
		{
			body = new Implies(new And(qualifiers), body);
		}

		return new ForAll(binds, body);
	}

	@Override
	public Expression caseExistsExpression(TCExistsExpression node, Environment env)
	{
		Bracketed binds = new Bracketed();
		Expression body = node.predicate.apply(this, env);
		List<Expression> qualifiers = new Vector<Expression>();
		
		for (TCMultipleBind bind: node.bindList)
		{
			if (bind instanceof TCMultipleTypeBind)
			{
				TCMultipleTypeBind mtbind = (TCMultipleTypeBind)bind;

				for (TCPattern pattern: mtbind.plist)
				{
					for (TCDefinition def: pattern.getDefinitions(mtbind.type, NameScope.LOCAL))
					{
						QualifiedSort qsort = def.getType().apply(new TypeToSMTConverter(def.name.getName()), env);
						binds.add(new Expression(def.name.getName(), qsort.sort));

						if (qsort.qualifier != null)
						{
							qualifiers.add(qsort.qualifier);
						}
					}
				}
			}
			else
			{
				throw new UnsupportedOperationException("Cannot convert: " + bind);
			}
		}

		if (qualifiers != null)
		{
			body = new Implies(new And(qualifiers), body);
		}

		return new Exists(binds, body);
	}

	@Override
	public Expression caseExpression(TCExpression node, Environment env)
	{
		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	@Override
	public Expression caseAbsoluteExpression(TCAbsoluteExpression node, Environment env)
	{
		return new Expression("abs", node.exp.apply(this, env));
	}

	@Override
	public Expression caseCardinalityExpression(TCCardinalityExpression node, Environment env)
	{
		return new Expression("set.card", node.exp.apply(this, env));
	}

	@Override
	public Expression caseSetUnionExpression(TCSetUnionExpression node, Environment env)
	{
		return new Expression("set.union",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseSetIntersectExpression(TCSetIntersectExpression node, Environment env)
	{
		return new Expression("set.inter",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseSetDifferenceExpression(TCSetDifferenceExpression node, Environment env)
	{
		return new Expression("set.minus",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseInSetExpression(TCInSetExpression node, Environment env)
	{
		return new Expression("set.member",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseNotInSetExpression(TCNotInSetExpression node, Environment env)
	{
		return new Not(
			new Expression("set.member",
				node.left.apply(this, env),
				node.right.apply(this, env)));
	}

	@Override
	public Expression caseSetEnumExpression(TCSetEnumExpression node, Environment env)
	{
		QualifiedSort qsort = node.getType().apply(new TypeToSMTConverter("?"), env);

		if (node.members.isEmpty())
		{
			return new Expression(new Text("as"), new Text("set.empty"), qsort.sort);
		}
		else if (node.members.size() == 1)
		{
			return new Expression(new Text("set.singleton"), node.members.get(0).apply(this, env));
		}
		else
		{
			// (set.insert <members> (as set.empty <sort>))
			Expression exp = new Expression("set.insert");

			for (TCExpression elem: node.members)
			{
				exp.add(elem.apply(this, env));
			}

			exp.add(new Expression(new Text("as"), new Text("set.empty"), qsort.sort));
			return exp;
		}
	}

	@Override
	public Expression caseSubsetExpression(TCSubsetExpression node, Environment env)
	{
		return new Expression("set.subset",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseProperSubsetExpression(TCProperSubsetExpression node, Environment env)
	{
		return new And(
			new Expression("set.subset",
				node.left.apply(this, env),
				node.right.apply(this, env)),
			new Not(
				new EQ(
					node.left.apply(this, env),
					node.right.apply(this, env))));
	}

	@Override
	public Expression caseIndicesExpression(TCIndicesExpression node, Environment env)
	{
		return new Expression("vdm.inds", node.exp.apply(this, env));
	}

	@Override
	public Expression caseElementsExpression(TCElementsExpression node, Environment env)
	{
		return new Expression("vdm.elems", node.exp.apply(this, env));
	}

	@Override
	public Expression caseLenExpression(TCLenExpression node, Environment env)
	{
		return new Expression("seq.len", node.exp.apply(this, env));
	}

	@Override
	public Expression caseSeqConcatExpression(TCSeqConcatExpression node, Environment env)
	{
		return new Expression(new Text("seq.++"),
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseSeqEnumExpression(TCSeqEnumExpression node, Environment env)
	{
		QualifiedSort qsort = node.getType().apply(new TypeToSMTConverter("?"), env);

		if (node.members.isEmpty())
		{
			return new Expression(new Text("as"),
				new Text("seq.empty"), qsort.sort);
		}
		else
		{
			Expression result = new Expression("seq.++");

			for (TCExpression e: node.members)
			{
				result.add(new Expression("seq.unit", e.apply(this, env)));
			}

			return result;
		}
	}

	@Override
	public Expression caseSubseqExpression(TCSubseqExpression node, Environment env)
	{
		return new Expression("seq.extract",
			node.seq.apply(this, env),
			new Expression("-", node.from.apply(this, env), new Text("1")),
			new Expression("-",
				node.to.apply(this, env),
				node.from.apply(this, env)));
	}

	@Override
	public Expression caseMapDomainExpression(TCMapDomainExpression node, Environment arg)
	{
		if (node.exp instanceof TCVariableExpression)
		{
			TCVariableExpression map = (TCVariableExpression)node.exp;
			return new Expression("dom_" + map.name.getName());
		}

		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	@Override
	public Expression caseFloorExpression(TCFloorExpression node, Environment env)
	{
		TCNumericType numeric = node.exp.getType().getNumeric();

		if (numeric.getWeight() < 3)	// ie. less than rat
		{
			return new Expression("floor",
				new Expression("to_real", node.exp.apply(this, env)));
		}
		else
		{
			return new Expression("floor", node.exp.apply(this, env));
		}
	}

	@Override
	public Expression casePlusExpression(TCPlusExpression node, Environment env)
	{
		return new Expression("+",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseSubtractExpression(TCSubtractExpression node, Environment env)
	{
		return new Expression("-",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseUnaryMinusExpression(TCUnaryMinusExpression node, Environment env)
	{
		return new Expression("-",
			node.exp.apply(this, env));
	}

	@Override
	public Expression caseUnaryPlusExpression(TCUnaryPlusExpression node, Environment env)
	{
		return node.exp.apply(this, env);
	}

	@Override
	public Expression caseTimesExpression(TCTimesExpression node, Environment env)
	{
		return new Expression("*",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseDivideExpression(TCDivideExpression node, Environment env)
	{
		return new Expression("/",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseDivExpression(TCDivExpression node, Environment env)
	{
		return new Expression("div",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseRemExpression(TCRemExpression node, Environment env)
	{
		return new Expression("rem",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseLetDefExpression(TCLetDefExpression node, Environment env)
	{
		Bracketed binds = new Bracketed();
		Expression body = node.expression.apply(this, env);
		List<Expression> qualifiers = new Vector<Expression>();
		
		for (TCDefinition def: node.localDefs)
		{
			if (def instanceof TCValueDefinition)
			{
				TCValueDefinition vdef = (TCValueDefinition)def;
				patternToExpression(vdef.pattern);	// Just to test it

				for (TCDefinition ldef: vdef.getDefinitions())
				{
					QualifiedSort qsort = ldef.getType().apply(new TypeToSMTConverter(ldef.name.getName()), env);
					binds.add(new Expression(ldef.name.getName(), vdef.exp.apply(this, env)));

					if (qsort.qualifier != null)
					{
						qualifiers.add(qsort.qualifier);
					}
				}
			}
			else
			{
				throw new UnsupportedOperationException("Cannot convert: " + def);
			}
		}

		if (qualifiers != null)
		{
			body = new Implies(new And(qualifiers), body);
		}

		return new Let(binds, body);
	}

	@Override
	public Expression caseModExpression(TCModExpression node, Environment env)
	{
		return new Expression("mod",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseEqualsExpression(TCEqualsExpression node, Environment env)
	{
		return new EQ(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseNotEqualExpression(TCNotEqualExpression node, Environment env)
	{
		return new Not(
			new EQ(
				node.left.apply(this, env),
				node.right.apply(this, env)));
	}

	@Override
	public Expression caseGreaterExpression(TCGreaterExpression node, Environment env)
	{
		return new GT(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseGreaterEqualExpression(TCGreaterEqualExpression node, Environment env)
	{
		return new GE(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseLessExpression(TCLessExpression node, Environment env)
	{
		return new LT(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseLessEqualExpression(TCLessEqualExpression node, Environment env)
	{
		return new LE(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseIfExpression(TCIfExpression node, Environment env)
	{
		Expression tail = node.elseExp.apply(this, env);

		if (!node.elseList.isEmpty())
		{
			ListIterator<TCElseIfExpression> iter = node.elseList.listIterator(node.elseList.size());
			
			while (iter.hasPrevious())
			{
				TCElseIfExpression elif = iter.previous();

				tail = new Expression("ite",
					elif.elseIfExp.apply(this, env),
					elif.thenExp.apply(this, env),
					tail);
			}
		}

		return new Expression("ite",
			node.ifExp.apply(this, env),
			node.thenExp.apply(this, env),
			tail);
	}

	@Override
	public Expression caseCasesExpression(TCCasesExpression node, Environment env)
	{
		Expression tail = null;

		if (node.others == null)
		{
			tail = new Expression("undefined");
		}
		else
		{
			tail = node.others.apply(this, env);
		}
	
		Expression test = node.exp.apply(this, env);

		ListIterator<TCCaseAlternative> iter = node.cases.listIterator(node.cases.size());
		
		while (iter.hasPrevious())
		{
			TCCaseAlternative calt = iter.previous();

			tail = new Expression("ite",
				new EQ(test, patternToExpression(calt.pattern)),
				calt.result.apply(this, env),
				tail);
		}

		return tail;
	}

	@Override
	public Expression caseAndExpression(TCAndExpression node, Environment env)
	{
		return new And(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseOrExpression(TCOrExpression node, Environment env)
	{
		return new Or(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseImpliesExpression(TCImpliesExpression node, Environment env)
	{
		return new Implies(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseNotExpression(TCNotExpression node, Environment env)
	{
		return new Not(node.exp.apply(this, env));
	}

	@Override
	public Expression caseVariableExpression(TCVariableExpression node, Environment env)
	{
		return new Expression(node.name.getName());
	}

	@Override
	public Expression caseIntegerLiteralExpression(TCIntegerLiteralExpression node, Environment env)
	{
		return new Expression(node.toString());
	}

	@Override
	public Expression caseBooleanLiteralExpression(TCBooleanLiteralExpression node, Environment env)
	{
		return new Expression(node.toString());
	}

	@Override
	public Expression caseRealLiteralExpression(TCRealLiteralExpression node, Environment arg)
	{
		return new Expression(node.toString());
	}

	@Override
	public Expression caseIsExpression(TCIsExpression node, Environment env)
	{
		TCNumericType numeric = node.test.getType().getNumeric();
		boolean isFloat = numeric.getWeight() > 2;

		if (node.basictype != null)
		{
			if (node.basictype instanceof TCNaturalType)
			{
				Expression test = node.test.apply(this, env);

				if (isFloat)
				{
					return new And(
						new EQ(new Expression("to_int", test), test),
						new GE(test, new Text("0")));
				}
				else
				{
					return new GE(test, new Text("0"));
				}
			}
			else if (node.basictype instanceof TCNaturalOneType)
			{
				Expression test = node.test.apply(this, env);

				if (isFloat)
				{
					return new And(
						new EQ(new Expression("to_int", test), test),
						new GT(test, new Text("0")));
				}
				else
				{
					return new GT(test, new Text("0"));
				}
			}
			else if (node.basictype instanceof TCRealType)
			{
				return new Expression("true");
			}
			else if (node.basictype instanceof TCBooleanType)
			{
				if (node.test instanceof TCApplyExpression)		// pre_ or post_
				{
					TCApplyExpression apply = (TCApplyExpression)node.test;
					TCExplicitFunctionDefinition def = (TCExplicitFunctionDefinition)findApplyDefinition(apply, env);
					return new Expression(def.type.partial ? "false" : "true");
				}
				else
				{
					return new Expression("true");		// Always?
				}
			}
		}
		
		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	@Override
	public Expression caseUndefinedExpression(TCUndefinedExpression node, Environment arg)
	{
		return new Expression("undefined");
	}

	private TCDefinition findApplyDefinition(TCApplyExpression node, Environment env)
	{
		if (node.root instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.root;
			return env.findName(var.name, NameScope.GLOBAL);
		}
		
		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	private boolean matchingArguments(TCExplicitFunctionDefinition def, TCApplyExpression apply)
	{
		if (def.paramPatternList.size() == 1)	// Not curried
		{
			if (def.paramPatternList.get(0).size() == apply.args.size())
			{
				for (int i=0; i < apply.args.size(); i++)
				{
					TCPattern p = def.paramPatternList.get(0).get(i);
					TCExpression a = apply.args.get(i);

					if (!p.toString().equals(a.toString()))
					{
						return false;
					}
				}

				return true;
			}
		}

		return false;
	}

	private Source patternToExpression(TCPattern pattern)
	{
		if (pattern instanceof TCIntegerPattern ||
			pattern instanceof TCRealPattern ||
			pattern instanceof TCBooleanPattern ||
			pattern instanceof TCIdentifierPattern)
		{
			return new Expression(pattern.toString());
		}
		else
		{
			throw new UnsupportedOperationException("Cannot convert pattern: " + pattern);
		}
	}
}
