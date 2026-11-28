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

import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCAbsoluteExpression;
import com.fujitsu.vdmj.tc.expressions.TCAndExpression;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCBinaryExpression;
import com.fujitsu.vdmj.tc.expressions.TCBooleanLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCCardinalityExpression;
import com.fujitsu.vdmj.tc.expressions.TCCaseAlternative;
import com.fujitsu.vdmj.tc.expressions.TCCasesExpression;
import com.fujitsu.vdmj.tc.expressions.TCCharLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCDistConcatExpression;
import com.fujitsu.vdmj.tc.expressions.TCDistIntersectExpression;
import com.fujitsu.vdmj.tc.expressions.TCDistMergeExpression;
import com.fujitsu.vdmj.tc.expressions.TCDistUnionExpression;
import com.fujitsu.vdmj.tc.expressions.TCElementsExpression;
import com.fujitsu.vdmj.tc.expressions.TCElseIfExpression;
import com.fujitsu.vdmj.tc.expressions.TCEqualsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCFieldExpression;
import com.fujitsu.vdmj.tc.expressions.TCFloorExpression;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCGreaterExpression;
import com.fujitsu.vdmj.tc.expressions.TCHeadExpression;
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
import com.fujitsu.vdmj.tc.expressions.TCMapInverseExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapRangeExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkBasicExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
import com.fujitsu.vdmj.tc.expressions.TCNilExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotInSetExpression;
import com.fujitsu.vdmj.tc.expressions.TCOrExpression;
import com.fujitsu.vdmj.tc.expressions.TCPowerSetExpression;
import com.fujitsu.vdmj.tc.expressions.TCProperSubsetExpression;
import com.fujitsu.vdmj.tc.expressions.TCQuoteLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCRealLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCReverseExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqConcatExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetDifferenceExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetIntersectExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetRangeExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetUnionExpression;
import com.fujitsu.vdmj.tc.expressions.TCStringLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubsetExpression;
import com.fujitsu.vdmj.tc.expressions.TCTailExpression;
import com.fujitsu.vdmj.tc.expressions.TCUnaryExpression;
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
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCRealPattern;
import com.fujitsu.vdmj.tc.patterns.TCRecordPattern;
import com.fujitsu.vdmj.tc.patterns.TCTuplePattern;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Pair;

import smtlib.ast.And;
import smtlib.ast.Bracketed;
import smtlib.ast.EQ;
import smtlib.ast.Exists;
import smtlib.ast.Expression;
import smtlib.ast.ExpressionList;
import smtlib.ast.ExpressionSet;
import smtlib.ast.ForAll;
import smtlib.ast.GE;
import smtlib.ast.GT;
import smtlib.ast.ITE;
import smtlib.ast.Implies;
import smtlib.ast.LE;
import smtlib.ast.LT;
import smtlib.ast.Let;
import smtlib.ast.Not;
import smtlib.ast.Or;
import smtlib.ast.Source;
import smtlib.ast.Text;
import smtlib.solvers.Solver;

public class ExpressionToSMTPair extends TCExpressionVisitor<SMTPair, Environment> implements SMTHelper
{
	private Solver solver = Solver.getInstance();

	@Override
	public SMTPair caseExpression(TCExpression node, Environment env)
	{
		throw new UnsupportedOperationException("Unsupported Pair type: " + node.getClass().getSimpleName());
	}

	@Override
	public SMTPair caseBinaryExpression(TCBinaryExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new Expression(node.op.toString(), left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	private SMTPair caseUnaryExpression(String op, TCUnaryExpression node, Environment env)
	{
		// TCUnaryExpression doesn't include the op token, so...
		SMTPair pair = node.exp.apply(this, env);
		return new SMTPair(new Expression(op, pair.value), pair.deltas);
	}

	@Override
	public SMTPair caseApplyExpression(TCApplyExpression node, Environment env)
	{
		TCDefinition root = null;

		if (node.root instanceof TCVariableExpression)	// Must be s(i)
		{
			TCVariableExpression var = (TCVariableExpression)node.root;
			root = env.findName(var.name, NameScope.NAMESANDSTATE);

			if (root == null && var.name.getName().startsWith("measure_"))
			{
				throw new UnsupportedOperationException("Missing measure definition: " + node);
			}
		}

		if (root == null)
		{
			throw new UnsupportedOperationException("Unknown definition: " + node);
		}

		if (root.name.getModule().equals(Globals.INTERNAL_MODULE))
		{
			Expression call = new Expression(root.name.getName());
			ExpressionSet deltas = new ExpressionSet();

			for (TCExpression arg: node.args)
			{
				SMTPair pair = arg.apply(this, env);
				call.add(pair.value);
				deltas.addAll(pair.deltas);
			}

			return new SMTPair(call, deltas);
		}
		else if (root instanceof TCExplicitFunctionDefinition)
		{
			TCExplicitFunctionDefinition exfn = (TCExplicitFunctionDefinition)root;
			
			if (exfn.isCurried)
			{
				throw new UnsupportedOperationException("Cannot expand curried fn: " + node);
			}

			DefinitionExpander.getInstance().expandFunction(exfn);

			if (exfn.paramPatternList.size() == 1)	// Not curried
			{
				ExpressionList args = new ExpressionList();
				ExpressionSet deltas = new ExpressionSet();

				for (TCExpression arg: node.args)
				{
					SMTPair pair = arg.apply(this, env);
					args.add(pair.value);
					deltas.addAll(pair.deltas);
				}

				Expression call = new Expression(exfn.name.getName());
				call.addAll(args);

				// Add a delta condition to call <name>_defined(args), which is
				// created from the SMTPair's delta when the function is translated.

				Expression call2 = new Expression(exfn.name.getName() + "_defined");
				call2.addAll(args);
				deltas.add(call2);

				if (exfn.predef != null)
				{
					Expression precall = new Expression();
					precall.addAll(call);
					precall.set(0, new Text(exfn.predef.name.getName()));
					deltas.add(precall);
				}

				if (exfn.postdef != null)
				{
					Expression postcall = new Expression();
					postcall.addAll(call);
					postcall.set(0, new Text(exfn.postdef.name.getName()));
					postcall.add(call);
					deltas.add(postcall);
				}

				return new SMTPair(call, deltas);
			}
		}
		else if (root instanceof TCImplicitFunctionDefinition)
		{
			TCImplicitFunctionDefinition imfn = (TCImplicitFunctionDefinition)root;
			DefinitionExpander.getInstance().expandFunction(imfn);

			Expression call = new Expression(imfn.name.getName());
			ExpressionSet deltas = new ExpressionSet();

			for (TCExpression arg: node.args)
			{
				SMTPair pair = arg.apply(this, env);
				call.add(pair.value);
				deltas.addAll(pair.deltas);
			}

			// Add a delta condition to call <name>_defined(args), which is
			// created from the SMTPair's delta when the function is translated.

			deltas.add(new Expression(imfn.name.getName() + "_defined"));

			if (imfn.predef != null)
			{
				Expression precall = new Expression();
				precall.addAll(call);
				precall.set(0, new Text(imfn.predef.name.getName()));
				deltas.add(precall);
			}

			if (imfn.postdef != null)
			{
				Expression postcall = new Expression();
				postcall.addAll(call);
				postcall.set(0, new Text(imfn.postdef.name.getName()));
				postcall.add(call);
				deltas.add(postcall);
			}

			return new SMTPair(call, deltas);
		}
		else if (node.root.getType().isSeq(node.location))
		{
			SMTPair sequence = node.root.apply(this, env);
			SMTPair index = node.args.get(0).apply(this, env);

			// (seq.nth s (- i 1)
			Expression result = new Expression(new Text("seq.nth"),
				sequence.value,
				new Expression(new Text("-"), index.value, new Text("1")));

			ExpressionSet deltas = new ExpressionSet(sequence.deltas, index.deltas);

			// s(i) only defined when 0 < i <= len s
			deltas.add(new And(
					new LT(new Text("0"), index.value),
					new LE(index.value, new Expression("seq.len", sequence.value))));

			return new SMTPair(result, deltas);
		}
		else if (node.root.getType().isMap(node.location))
		{
			SMTPair map = node.root.apply(this, env);
			SMTPair key = node.args.get(0).apply(this, env);

			// (select m k)
			Expression result = new Expression("select", map.value, key.value);

			ExpressionSet deltas = new ExpressionSet(map.deltas, key.deltas);

			// m(k) only defined when k in set dom m
			deltas.add(solver.setMember(
					key.value,
					new Expression("vdm.dom", map.value)));

			return new SMTPair(result, deltas);
		}

		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	/**
	 * Literals.
	 */

	@Override
	public SMTPair caseVariableExpression(TCVariableExpression node, Environment env)
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
	public SMTPair caseRealLiteralExpression(TCRealLiteralExpression node, Environment env)
	{
		return new SMTPair(new Expression(Double.toString(node.value.value)));
	}

	@Override
	public SMTPair caseCharLiteralExpression(TCCharLiteralExpression node, Environment env)
	{
		return new SMTPair(new Expression(Long.toString(node.value.unicode)));
	}

	@Override
	public SMTPair caseStringLiteralExpression(TCStringLiteralExpression node, Environment env)
	{
		return new SMTPair(new Expression("\"" + node.toString() + "\""));	// "quoted value"
	}

	@Override
	public SMTPair caseQuoteLiteralExpression(TCQuoteLiteralExpression node, Environment env)
	{
		return new SMTPair(new Expression(node.type.value));	// eg. <ABC> is ABC
	}

	@Override
	public SMTPair caseUndefinedExpression(TCUndefinedExpression node, Environment arg)
	{
		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	@Override
	public SMTPair caseNilExpression(TCNilExpression node, Environment arg)
	{
		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	/**
	 * Numerics.
	 */

	@Override
	public SMTPair caseAbsoluteExpression(TCAbsoluteExpression node, Environment env)
	{
		return caseUnaryExpression("abs", node, env);
	}

	@Override
	public SMTPair caseFloorExpression(TCFloorExpression node, Environment env)
	{
		SMTPair exp = node.exp.apply(this, env);
		return new SMTPair(new Expression("to_int", exp.value), exp.deltas);
	}

	@Override
	public SMTPair caseUnaryMinusExpression(TCUnaryMinusExpression node, Environment env)
	{
		return caseUnaryExpression("-", node, env);
	}

	@Override
	public SMTPair caseUnaryPlusExpression(TCUnaryPlusExpression node, Environment env)
	{
		return caseUnaryExpression("+", node, env);
	}

	@Override
	public SMTPair caseEqualsExpression(TCEqualsExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new EQ(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseNotEqualExpression(TCNotEqualExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new Not(new EQ(left.value, right.value)),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseGreaterExpression(TCGreaterExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new GT(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseGreaterEqualExpression(TCGreaterEqualExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new GE(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseLessExpression(TCLessExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new LT(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseLessEqualExpression(TCLessEqualExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new LE(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	/**
	 * Booleans
	 */

	@Override
	public SMTPair caseAndExpression(TCAndExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new And(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseOrExpression(TCOrExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new Or(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseImpliesExpression(TCImpliesExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new Implies(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseNotExpression(TCNotExpression node, Environment env)
	{
		SMTPair exp = node.exp.apply(this, env);

		return new SMTPair(
			new Not(exp.value),
			new ExpressionSet(exp.deltas));
	}

	/**
	 * Sets.
	 */

	@Override
	public SMTPair caseCardinalityExpression(TCCardinalityExpression node, Environment env)
	{
		SMTPair set = node.exp.apply(this, env);
		return new SMTPair(solver.setCard(set.value), set.deltas);
	}

	@Override
	public SMTPair caseSetUnionExpression(TCSetUnionExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			solver.setUnion(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseSetIntersectExpression(TCSetIntersectExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			solver.setInter(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseSetDifferenceExpression(TCSetDifferenceExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			solver.setMinus(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseInSetExpression(TCInSetExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			solver.setMember(left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseNotInSetExpression(TCNotInSetExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new Not(solver.setMember(left.value, right.value)),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseSetRangeExpression(TCSetRangeExpression node, Environment env)
	{
		if (node.first instanceof TCIntegerLiteralExpression &&
			node.last instanceof TCIntegerLiteralExpression)
		{
			TCIntegerLiteralExpression first = (TCIntegerLiteralExpression)node.first;
			TCIntegerLiteralExpression last = (TCIntegerLiteralExpression)node.last;
			TCExpressionList list = new TCExpressionList();

			for (long i=first.value.value; i <= last.value.value; i++)
			{
				list.add(new TCIntegerLiteralExpression(new LexIntegerToken(i, LexLocation.ANY)));
			}

			return setElems(list, node.getType(), env);
		}
		else
		{
			return caseExpression(node, env);
		}
	}

	@Override
	public SMTPair caseSetEnumExpression(TCSetEnumExpression node, Environment env)
	{
		return setElems(node.members, node.getType(), env);
	}

	private SMTPair setElems(TCExpressionList members, TCType type, Environment env)
	{
		QualifiedSort qsort = type.apply(new TypeToSMT("?"), env);

		if (members.isEmpty())
		{
			return new SMTPair(new Expression("as", new Text("set.empty"), qsort.sort));
		}
		else if (members.size() == 1)
		{
			SMTPair singleton = members.get(0).apply(this, env);

			return new SMTPair(
				new Expression("set.singleton", singleton.value),
				singleton.deltas);
		}
		else
		{
			// (set.insert <members> (as set.empty <sort>))
			List<Source> elems = new Vector<Source>();
			ExpressionSet deltas = new ExpressionSet();

			for (TCExpression elem: members)
			{
				SMTPair pair = elem.apply(this, env);
				elems.add(pair.value);
				deltas.addAll(pair.deltas);
			}

			elems.add(new Expression("as", new Text("set.empty"), qsort.sort));
			return new SMTPair(solver.setInsert(elems), deltas);
		}
	}

	@Override
	public SMTPair caseSubsetExpression(TCSubsetExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new Expression("set.subset", left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseProperSubsetExpression(TCProperSubsetExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new And(
				new Expression("set.subset", left.value, right.value),
				new Not(new EQ(left.value, right.value))),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseDistUnionExpression(TCDistUnionExpression node, Environment env)
	{
		return caseUnaryExpression("vdm.dunion", node, env);
	}

	@Override
	public SMTPair caseDistIntersectExpression(TCDistIntersectExpression node, Environment env)
	{
		return caseUnaryExpression("vdm.dinter", node, env);
	}

	@Override
	public SMTPair casePowerSetExpression(TCPowerSetExpression node, Environment env)
	{
		return caseUnaryExpression("vdm.power", node, env);
	}

	/**
	 * Maps.
	 */

	@Override
	public SMTPair caseDistMergeExpression(TCDistMergeExpression node, Environment env)
	{
		return caseUnaryExpression("merge", node, env);
	}

	@Override
	public SMTPair caseMapDomainExpression(TCMapDomainExpression node, Environment env)
	{
		return caseUnaryExpression("dom", node, env);
	}

	@Override
	public SMTPair caseMapInverseExpression(TCMapInverseExpression node, Environment env)
	{
		return caseUnaryExpression("inverse", node, env);
	}

	@Override
	public SMTPair caseMapRangeExpression(TCMapRangeExpression node, Environment env)
	{
		return caseUnaryExpression("rng", node, env);
	}

	/**
	 * Sequences.
	 */

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
			return new SMTPair(new Expression("seq.unit", first.value), first.deltas);
		}
		else
		{
			Expression result = new Expression("seq.++");
			ExpressionSet deltas = new ExpressionSet();

			for (TCExpression e: node.members)
			{
				SMTPair pair = e.apply(this, env);
				result.add(new Expression("seq.unit", pair.value));
				deltas.addAll(pair.deltas);
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
					seq.deltas,
					new GT(
						new Expression("seq.len", seq.value),
						new Text("0"))));
	}

	@Override
	public SMTPair caseTailExpression(TCTailExpression node, Environment env)
	{
		// (seq.extract s 1 (- (seq.len s) 1)))
		SMTPair seq = node.exp.apply(this, env);

		return new SMTPair(
			 new Expression(new Text("seq.extract"),
				seq.value,
				new Text("1"),
				new Expression("-",
					new Expression("seq.len", seq.value),
					new Text("1"))),
			new ExpressionSet(
					seq.deltas,
					new GT(
						new Expression("seq.len", seq.value),
						new Text("0"))));
	}

	@Override
	public SMTPair caseIndicesExpression(TCIndicesExpression node, Environment env)
	{
		if (node.exp instanceof TCSeqEnumExpression)
		{
			TCSeqEnumExpression seq = (TCSeqEnumExpression)node.exp;

			if (seq.members.isEmpty())
			{
				// We need to know the type of the elements... 
				throw new UnsupportedOperationException("Cannot convert empty set");
			}

			if (seq.members.size() == 1)
			{
				return new SMTPair(new Expression("set.singleton", "1"));
			}
			else
			{
				// (set.insert <1...n> (as set.empty Int))
				List<Source> elems = new Vector<Source>();

				for (int indx=1; indx <= seq.members.size(); indx++)
				{
					elems.add(new Text(Integer.toString(indx)));
				}

				elems.add(new Expression(
					"as",
					new Text("set.empty"),
					solver.setType(new Text("Int"))));

				return new SMTPair(solver.setInsert(elems));
			}
		}
		else
		{
			SMTPair seq = node.exp.apply(this, env);

			return new SMTPair(
				new Expression("vdm.inds", seq.value),
				seq.deltas);
		}
	}

	@Override
	public SMTPair caseElementsExpression(TCElementsExpression node, Environment env)
	{
		if (node.exp instanceof TCSeqEnumExpression)
		{
			TCSeqEnumExpression seq = (TCSeqEnumExpression)node.exp;
			TCSeqType seqtype = seq.getType().getSeq();
			TCSetType settype = new TCSetType(LexLocation.ANY, seqtype.seqof);

			return setElems(seq.members, settype, env);
		}
		else
		{
			SMTPair set = node.exp.apply(this, env);

			return new SMTPair(
				new Expression("vdm.elems", set.value),
				set.deltas);
		}
	}

	@Override
	public SMTPair caseLenExpression(TCLenExpression node, Environment env)
	{
		return caseUnaryExpression("seq.len", node, env);
	}

	@Override
	public SMTPair caseSeqConcatExpression(TCSeqConcatExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new Expression("seq.++", left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
	}

	@Override
	public SMTPair caseDistConcatExpression(TCDistConcatExpression node, Environment env)
	{
		return caseUnaryExpression("vdm.conc", node, env);
	}

	@Override
	public SMTPair caseReverseExpression(TCReverseExpression node, Environment env)
	{
		return caseUnaryExpression("vdm.reverse", node, env);
	}

	/**
	 * Records and tuples.
	 */

	@Override
	public SMTPair caseMkTypeExpression(TCMkTypeExpression node, Environment env)
	{
		Expression mkexp = new Expression(new Text("mk_" + node.typename));
		ExpressionSet deltas = new ExpressionSet();

		for (TCExpression arg: node.args)
		{
			SMTPair pair = arg.apply(this, env);
			mkexp.add(pair.value);
			deltas.addAll(pair.deltas);
		}

		return new SMTPair(mkexp, deltas);
	}

	@Override
	public SMTPair caseMkBasicExpression(TCMkBasicExpression node, Environment arg)
	{
		if (node.type instanceof TCTokenType)
		{
			return new SMTPair(
				new Expression(String.valueOf(node.arg.toString().hashCode())));	// raw token value
		}
		else if (node.type instanceof TCIntegerType)
		{
			return node.arg.apply(this, arg);
		}

		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	@Override
	public SMTPair caseFieldExpression(TCFieldExpression node, Environment env)
	{
		SMTPair object = node.object.apply(this, env);
		return new SMTPair(new Expression(new Text(node.field.getName()), object.value), object.deltas);
	}

	/**
	 * Conditionals
	 */

	@Override
	public SMTPair caseIsExpression(TCIsExpression node, Environment env)
	{
		if (node.basictype != null)
		{
			TCNumericType numeric = node.test.getType().getNumeric();
			boolean isNumeric = numeric != null;
			boolean isFloat = isNumeric && numeric.getWeight() > 2;
			boolean isBool = node.test.getType() instanceof TCBooleanType;

			if (node.basictype instanceof TCNaturalType)
			{
				Expression test = node.test.apply(this, env).value;

				if (isFloat)
				{
					return new SMTPair(new And(
						new EQ(new Expression("to_int", test), test),
						new GE(test, new Text("0"))));
				}
				else if (isNumeric)
				{
					return new SMTPair(new GE(test, new Text("0")));
				}
				else
				{
					return new SMTPair(new Expression("false"));
				}
			}
			else if (node.basictype instanceof TCNaturalOneType)
			{
				Expression test = node.test.apply(this, env).value;

				if (isFloat)
				{
					return new SMTPair(new And(
						new EQ(new Expression("to_int", test), test),
						new GT(test, new Text("0"))));
				}
				else if (isNumeric)
				{
					return new SMTPair(new GT(test, new Text("0")));
				}
				else
				{
					return new SMTPair(new Expression("false"));
				}
			}
			else if (node.basictype instanceof TCRealType)
			{
				return new SMTPair(new Expression(Boolean.toString(isFloat)));
			}
			else if (node.basictype instanceof TCBooleanType)
			{
				if (node.test instanceof TCApplyExpression)		// pre_ or post_
				{
					TCApplyExpression apply = (TCApplyExpression)node.test;

					if (apply.type instanceof TCFunctionType)
					{
						TCFunctionType ftype = (TCFunctionType)apply.type;
						return new SMTPair(new Expression(Boolean.toString(
							ftype.result instanceof TCBooleanType && !ftype.partial)));
					}
				}
				
				return new SMTPair(new Expression(Boolean.toString(isBool)));
			}
		}
		
		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	@Override
	public SMTPair caseIfExpression(TCIfExpression node, Environment env)
	{
		SMTPair ifExp = node.ifExp.apply(this, env);
		SMTPair thenExp = node.thenExp.apply(this, env);
		SMTPair elseExp = node.elseExp.apply(this, env);

		Expression tail = elseExp.value;
		Expression tailDelta = elseExp.deltas.andOf();

		ListIterator<TCElseIfExpression> iter =
			node.elseList.listIterator(node.elseList.size());

		while (iter.hasPrevious())
		{
			TCElseIfExpression elif = iter.previous();
			SMTPair ifex = elif.elseIfExp.apply(this, env);
			SMTPair thenex = elif.thenExp.apply(this, env);

			tailDelta = new And(ifex.deltas.andOf(),
								new ITE(ifex.value, thenex.deltas.andOf(), tailDelta));

			tail = new ITE(ifex.value, thenex.value, tail);
		}

		ITE value = new ITE(ifExp.value, thenExp.value, tail);

		ExpressionSet deltas = new ExpressionSet(ifExp.deltas);
		deltas.add(new ITE(ifExp.value, thenExp.deltas.andOf(), tailDelta));

		return new SMTPair(value, deltas);
	}

	@Override
	public SMTPair caseCasesExpression(TCCasesExpression node, Environment env)
	{
		SMTPair test = node.exp.apply(this, env);
		ExpressionSet deltas = new ExpressionSet(test.deltas);

		Expression tail = null;
		Expression tailDelta = null;

		if (node.others != null)
		{
			SMTPair others = node.others.apply(this, env);
			tail = others.value;
			tailDelta = others.deltas.andOf();
		}

		ExpressionSet allcases = new ExpressionSet();
		ListIterator<TCCaseAlternative> iter = node.cases.listIterator(node.cases.size());

		while (iter.hasPrevious())
		{
			TCCaseAlternative calt = iter.previous();
			TCDefinitionList defs = calt.pattern.getAllDefinitions(node.exp.getType(), NameScope.LOCAL);
			FlatEnvironment local = new FlatEnvironment(defs, env);
			SMTPair pair = calt.result.apply(this, local);
			EQ match = new EQ(test.value, patternToExpression(calt.pattern));

			allcases.add(match);

			if (tail == null)       // No others clause: last case is the fallback
			{
				tail = pair.value;
				tailDelta = pair.deltas.andOf();
			}
			else
			{
				tail = new ITE(match, pair.value, tail);
				tailDelta = new ITE(match, pair.deltas.andOf(), tailDelta);
			}
		}

		if (node.others == null)
		{
			deltas.add(allcases.orOf());    // Must match one clause
		}

		deltas.add(tailDelta);
		return new SMTPair(tail, deltas);
	}

	/**
	 * Quantifiers
	 */

	@Override
	public SMTPair caseForAllExpression(TCForAllExpression node, Environment env)
	{
		Pair<Bracketed, SMTPair> pair = expandQuantifier(true, node.bindList, node.predicate, env);
		return new SMTPair(new ForAll(pair.first, pair.second.value), pair.second.deltas);
	}

	@Override
	public SMTPair caseExistsExpression(TCExistsExpression node, Environment env)
	{
		Pair<Bracketed, SMTPair> pair = expandQuantifier(false, node.bindList, node.predicate, env);
		return new SMTPair(new Exists(pair.first, pair.second.value), pair.second.deltas);
	}

	private Pair<Bracketed, SMTPair> expandQuantifier(
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

		// [<qualifiers> (=>/and) [<deltas> and]] <translated body>

		body = body.andDeltas();

		if (forall)
		{
			body = body.impliedBy(qualifiers);
		}
		else
		{
			body = body.andWith(qualifiers);
		}

		if (!letquals.isEmpty())
		{
			body = Utils.combineLets(letquals, body.value, env);
		}

		return new Pair<Bracketed, SMTPair>(newBindList, body);
	}

	/**
	 * Let/def expressions
	 */

	@Override
	public SMTPair caseLetDefExpression(TCLetDefExpression node, Environment env)
	{
		return expandLetDef(node.localDefs, node.expression, env);
	}

	@Override
	public SMTPair caseDefExpression(TCDefExpression node, Environment env)
	{
		return expandLetDef(node.localDefs, node.expression, env);
	}

	private SMTPair expandLetDef(
		TCDefinitionList localDefs, TCExpression expression, Environment env)
	{
		ExpressionSet qualifiers = new ExpressionSet();
		List<Bracketed> newBindList = new Vector<Bracketed>();
		TCExpressionList letquals = new TCExpressionList();

		for (TCDefinition def: localDefs)
		{
			if (def instanceof TCValueDefinition)
			{
				TCValueDefinition vdef = (TCValueDefinition)def;
				String varname = null;

				if (vdef.pattern instanceof TCIdentifierPattern)
				{
					TCIdentifierPattern id = (TCIdentifierPattern)vdef.pattern;
					varname = id.toString();
				}
				else if (vdef.pattern instanceof TCRecordPattern)
				{
					TCRecordPattern rp = (TCRecordPattern)vdef.pattern;
					TCType ptype = vdef.getType();
					TCIdentifierPattern p2 = Utils.checkPattern(rp, ptype, letquals, env);
					varname = p2.toString();
				}
				else if (vdef.pattern instanceof TCTuplePattern)
				{
					TCTuplePattern rp = (TCTuplePattern)vdef.pattern;
					TCType ptype = vdef.getType();
					TCIdentifierPattern p2 = Utils.checkPattern(rp, ptype, letquals, env);
					varname = p2.toString();
				}
				else
				{
					throw new UnsupportedOperationException("Cannot expand pattern: " + def);
				}

				QualifiedSort qsort = vdef.getType().apply(new TypeToSMT(varname), env);

				if (qsort.qualifier != null)
				{
					qualifiers.add(qsort.qualifier);
				}

				SMTPair pair = vdef.exp.apply(this, env);
				newBindList.add(0, new Bracketed(new Text(varname), pair.value));
			}
			else
			{
				throw new UnsupportedOperationException("Cannot expand let: " + def);
			}
		}

		SMTPair body = expression.apply(this, env);
		body.deltas.addAll(qualifiers);

		// Create a let cascade for all the bindings
		Expression resultValue = letCascade(newBindList, body.value);
		ExpressionSet resultDeltas = new ExpressionSet();

		if (!body.deltas.isEmpty())
		{
			// Add a let clause to the body delta, so we can propagate up,
			resultDeltas.add(letCascade(newBindList, body.deltas.andOf()));
		}

		if (!letquals.isEmpty())
		{
			// Adds deltas for the rhs of the pattern let expressions
			SMTPair lets = Utils.combineLets(letquals, body.value, env);
			resultValue = lets.value;
			resultDeltas.addAll(lets.deltas);	// Outside the delta's let
		}

		return new SMTPair(resultValue, resultDeltas);
	}

	/**
	 * Private methods
	 */
	private Let letCascade(List<Bracketed> binds, Expression body)
	{
		Let cascade = null;

		for (Bracketed bind: binds)	// Keep lets in order
		{
			if (cascade == null)
			{
				cascade = new Let(bind, body);
			}
			else
			{
				cascade = new Let(bind, cascade);
			}
		}

		return cascade;
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
