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
import com.fujitsu.vdmj.tc.expressions.TCBooleanLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCCardinalityExpression;
import com.fujitsu.vdmj.tc.expressions.TCCaseAlternative;
import com.fujitsu.vdmj.tc.expressions.TCCasesExpression;
import com.fujitsu.vdmj.tc.expressions.TCCharLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCDivExpression;
import com.fujitsu.vdmj.tc.expressions.TCDivideExpression;
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
import com.fujitsu.vdmj.tc.expressions.TCMapRangeExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkBasicExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
import com.fujitsu.vdmj.tc.expressions.TCModExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotInSetExpression;
import com.fujitsu.vdmj.tc.expressions.TCOrExpression;
import com.fujitsu.vdmj.tc.expressions.TCPlusExpression;
import com.fujitsu.vdmj.tc.expressions.TCPostOpExpression;
import com.fujitsu.vdmj.tc.expressions.TCPreOpExpression;
import com.fujitsu.vdmj.tc.expressions.TCProperSubsetExpression;
import com.fujitsu.vdmj.tc.expressions.TCRealLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCRemExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqConcatExpression;
import com.fujitsu.vdmj.tc.expressions.TCSeqEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetDifferenceExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetEnumExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetIntersectExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetRangeExpression;
import com.fujitsu.vdmj.tc.expressions.TCSetUnionExpression;
import com.fujitsu.vdmj.tc.expressions.TCStringLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubseqExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubsetExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubtractExpression;
import com.fujitsu.vdmj.tc.expressions.TCTailExpression;
import com.fujitsu.vdmj.tc.expressions.TCTimesExpression;
import com.fujitsu.vdmj.tc.expressions.TCUnaryMinusExpression;
import com.fujitsu.vdmj.tc.expressions.TCUnaryPlusExpression;
import com.fujitsu.vdmj.tc.expressions.TCUndefinedExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.TCBooleanPattern;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCIntegerPattern;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCRealPattern;
import com.fujitsu.vdmj.tc.patterns.TCRecordPattern;
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
import smtlib.solvers.Solver;

public class ExpressionToSMT extends TCExpressionVisitor<Expression, Environment>
{
	private final Solver solver;

	public ExpressionToSMT()
	{
		this.solver = Solver.getInstance();
	}

	@Override
	public Expression caseExpression(TCExpression node, Environment env)
	{
		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	@Override
	public Expression caseApplyExpression(TCApplyExpression node, Environment env)
	{
		TCDefinition root = null;

		if (node.root instanceof TCVariableExpression)	// Must be s(i)
		{
			TCVariableExpression var = (TCVariableExpression)node.root;
			root = env.findName(var.name, NameScope.NAMESANDSTATE);
		}

		if (root instanceof TCExplicitFunctionDefinition)
		{
			TCExplicitFunctionDefinition def = (TCExplicitFunctionDefinition)root;

			if (def.paramPatternList.size() == 1)	// Not curried
			{
				Expression call = new Expression(def.name.getName());

				for (TCExpression arg: node.args)
				{
					call.add(arg.apply(this, env));
				}

				return call;
			}
		}
		else if (root instanceof TCImplicitFunctionDefinition)
		{
			TCImplicitFunctionDefinition def = (TCImplicitFunctionDefinition)root;
			Expression call = new Expression(def.name.getName());

			for (TCExpression arg: node.args)
			{
				call.add(arg.apply(this, env));
			}

			return call;
		}
		else if (node.root.getType().isSeq(node.location))
		{
			Expression sequence = node.root.apply(this, env);
			Expression index = node.args.get(0).apply(this, env);

			// (seq.nth s (- i 1)
			Expression result = new Expression(new Text("seq.nth"),
				sequence,
				new Expression(new Text("-"), index, new Text("1")));

			return result;
		}
		else if (node.root.getType().isMap(node.location))
		{
			Expression map = node.root.apply(this, env);
			Expression key = node.args.get(0).apply(this, env);

			// (select m k)
			Expression result = new Expression("select", map, key);

			return result;
		}

		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	@Override
	public Expression caseForAllExpression(TCForAllExpression node, Environment env)
	{
		Pair<Bracketed,Expression> pair = qualifierToSMT(node.bindList, node.predicate, env);
		return new ForAll(pair.first, pair.second);
	}

	@Override
	public Expression caseExistsExpression(TCExistsExpression node, Environment env)
	{
		Pair<Bracketed,Expression> pair = qualifierToSMT(node.bindList, node.predicate, env);
		return new Exists(pair.first, pair.second);
	}

	private Pair<Bracketed, Expression> qualifierToSMT(TCMultipleBindList bindList,
		TCExpression predicate, Environment env)
	{
		TCDefinitionList localdefs = new TCDefinitionList();
		
		for (TCMultipleBind bind: bindList)
		{
			if (bind instanceof TCMultipleTypeBind)
			{
				TCMultipleTypeBind mtbind = (TCMultipleTypeBind)bind;

				for (TCPattern pattern: mtbind.plist)
				{
					localdefs.addAll(pattern.getDefinitions(mtbind.type, NameScope.LOCAL));
				}
			}
			else if (bind instanceof TCMultipleSeqBind)
			{
				TCMultipleSeqBind msbind = (TCMultipleSeqBind)bind;
				TCSeqType stype = msbind.sequence.getType().getSeq();

				for (TCPattern pattern: msbind.plist)
				{
					localdefs.addAll(pattern.getDefinitions(stype.seqof, NameScope.LOCAL));
				}
			}
			else if (bind instanceof TCMultipleSetBind)
			{
				TCMultipleSetBind msbind = (TCMultipleSetBind)bind;
				TCSetType stype = msbind.set.getType().getSet();

				for (TCPattern pattern: msbind.plist)
				{
					localdefs.addAll(pattern.getDefinitions(stype.setof, NameScope.LOCAL));
				}
			}
			else
			{
				throw new UnsupportedOperationException("Cannot convert: " + bind);
			}
		}

		Bracketed binds = new Bracketed();

		for (TCDefinition def: localdefs)
		{
			QualifiedSort stype = def.getType().apply(new TypeToSMT("?"), env);
			binds.add(new Expression(def.name.getName(), stype.sort));
		}

		FlatEnvironment local = new FlatEnvironment(localdefs, env);
		Expression body = predicate.apply(this, local);

		return new Pair<Bracketed, Expression>(binds, body);
	}

	@Override
	public Expression caseLetDefExpression(TCLetDefExpression node, Environment env)
	{
		Bracketed binds = new Bracketed();

		for (TCDefinition def: node.localDefs)
		{
			if (def instanceof TCValueDefinition)
			{
				TCValueDefinition vdef = (TCValueDefinition)def;

				if (vdef.pattern instanceof TCIdentifierPattern)
				{
					String var = vdef.pattern.toString();
					binds.add(new Expression(var, vdef.exp.apply(this, env)));
				}
				else if (vdef.pattern instanceof TCRecordPattern)
				{
					throw new UnsupportedOperationException("Cannot convert record: " + def);
				}
				else
				{
					throw new UnsupportedOperationException("Cannot convert pattern: " + def);
				}
			}
			else
			{
				throw new UnsupportedOperationException("Cannot convert let: " + def);
			}
		}

		FlatEnvironment local = new FlatEnvironment(node.localDefs, env);
		Expression body = node.expression.apply(this, local);

		return new Let(binds, body);
	}

	@Override
	public Expression caseCardinalityExpression(TCCardinalityExpression node, Environment env)
	{
		return solver.setCard(node.exp.apply(this, env));
	}

	@Override
	public Expression caseSetUnionExpression(TCSetUnionExpression node, Environment env)
	{
		return solver.setUnion(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseSetIntersectExpression(TCSetIntersectExpression node, Environment env)
	{
		return solver.setInter(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseSetDifferenceExpression(TCSetDifferenceExpression node, Environment env)
	{
		return solver.setMinus(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseInSetExpression(TCInSetExpression node, Environment env)
	{
		return solver.setMember(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseNotInSetExpression(TCNotInSetExpression node, Environment env)
	{
		return new Not(
			solver.setMember(
				node.left.apply(this, env),
				node.right.apply(this, env)));
	}

	@Override
	public Expression caseSetRangeExpression(TCSetRangeExpression node, Environment env)
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
	public Expression caseSetEnumExpression(TCSetEnumExpression node, Environment env)
	{
		return setElems(node.members, node.getType(), env);
	}

	private Expression setElems(TCExpressionList members, TCType type, Environment env)
	{
		QualifiedSort qsort = type.apply(new TypeToSMT("?"), env);

		if (members.isEmpty())
		{
			return new Expression("as", new Text("set.empty"), qsort.sort);
		}
		else if (members.size() == 1)
		{
			return new Expression("set.singleton", members.get(0).apply(this, env));
		}
		else
		{
			// (set.insert <members> (as set.empty <sort>))
			List<Source> elems = new Vector<Source>();

			for (TCExpression elem: members)
			{
				elems.add(elem.apply(this, env));
			}

			elems.add(new Expression("as", new Text("set.empty"), qsort.sort));
			return solver.setInsert(elems);
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
				return new Expression("set.singleton", "1");
			}
			else
			{
				// (set.insert <1...n> (as set.empty Int))
				List<Source> elems = new Vector<Source>();

				for (int indx=1; indx <= seq.members.size(); indx++)
				{
					elems.add(new Text(Integer.toString(indx)));
				}

				elems.add(new Expression("as", new Text("set.empty"), solver.setType(new Text("Int"))));
				return solver.setInsert(elems);
			}
		}
		else
		{
			return new Expression("vdm.inds", node.exp.apply(this, env));
		}
	}

	@Override
	public Expression caseElementsExpression(TCElementsExpression node, Environment env)
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
			return new Expression("vdm.elems", node.exp.apply(this, env));
		}
	}

	@Override
	public Expression caseLenExpression(TCLenExpression node, Environment env)
	{
		return new Expression("seq.len", node.exp.apply(this, env));
	}

	@Override
	public Expression caseSeqConcatExpression(TCSeqConcatExpression node, Environment env)
	{
		return new Expression("seq.++",
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	@Override
	public Expression caseSeqEnumExpression(TCSeqEnumExpression node, Environment env)
	{
		if (node.members.isEmpty())
		{
			QualifiedSort qsort = node.getType().apply(new TypeToSMT("?"), env);
			return new Expression("as", new Text("seq.empty"), qsort.sort);
		}
		else if (node.members.size() == 1)
		{
			return new Expression("seq.unit",
				node.members.firstElement().apply(this, env));
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
			new Expression("-",
				node.from.apply(this, env),
				new Text("1")),
			new Expression("-",
				node.to.apply(this, env),
				node.from.apply(this, env)));
	}

	@Override
	public Expression caseHeadExpression(TCHeadExpression node, Environment env)
	{
		return new Expression(new Text("seq.nth"),
				node.exp.apply(this, env),
				new Text("0"));
	}

	@Override
	public Expression caseTailExpression(TCTailExpression node, Environment env)
	{
		// (seq.extract s 1 (- (seq.len s) 1)))
		Expression seq = node.exp.apply(this, env);

		return new Expression(new Text("seq.extract"),
				seq,
				new Expression("-",
					new Expression("seq.len", seq),
					new Text("1")));
	}

	@Override
	public Expression caseMapDomainExpression(TCMapDomainExpression node, Environment env)
	{
		return new Expression("vdm.dom", node.exp.apply(this, env));
	}

	@Override
	public Expression caseMapRangeExpression(TCMapRangeExpression node, Environment env)
	{
		return new Expression("vdm.rng", node.exp.apply(this, env));
	}

	@Override
	public Expression caseAbsoluteExpression(TCAbsoluteExpression node, Environment env)
	{
		return new Expression("abs", node.exp.apply(this, env));
	}

	@Override
	public Expression caseFloorExpression(TCFloorExpression node, Environment env)
	{
		return new Expression("to_int", node.exp.apply(this, env));
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
			TCType rtype = node.cases.firstElement().result.getType();
			QualifiedSort qsort = rtype.apply(new TypeToSMT("?"), env);
			tail = new Expression("undefined" + qsort.sort);
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

			TCDefinitionList defs = calt.pattern.getAllDefinitions(node.exp.getType(), NameScope.LOCAL);
			FlatEnvironment local = new FlatEnvironment(defs, env);

			tail = new Expression("ite",
				new EQ(test, patternToExpression(calt.pattern)),
				calt.result.apply(this, local),
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

	/**
	 * Note that we constrain the RHS, because we may not evaluate that side,
	 * by McCarthy logic. So SMT is emulating the interpreter...?
	 */
	@Override
	public Expression caseOrExpression(TCOrExpression node, Environment env)
	{
		return new Or(
			node.left.apply(this, env),
			node.right.apply(this, env));
	}

	/**
	 * Note that we constrain the RHS, because its limits may not apply
	 * outside, within the LHS.
	 */
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
		TCDefinition def = env.findName(node.name, NameScope.NAMESANDSTATE);
		String var = node.name.getName();

		if (def == null)
		{
			throw new UnsupportedOperationException("No definition for " + var);
		}

		QualifiedSort qsort = def.getType().apply(new TypeToSMT(var), env);
		Expression exp = new Expression(var);

		if (qsort.qualifier != null)
		{
			qsort.qualifier.setComment(var + ":" + def.getType());
		}

		return exp;
	}

	@Override
	public Expression caseFieldExpression(TCFieldExpression node, Environment env)
	{
		return new Expression(new Text(node.field.getName()), node.object.apply(this, env));
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
	public Expression caseCharLiteralExpression(TCCharLiteralExpression node, Environment arg)
	{
		return new Expression(Character.toString(node.value.unicode));	// eg. 'A' becomes 65
	}

	@Override
	public Expression caseStringLiteralExpression(TCStringLiteralExpression node, Environment arg)
	{
		return new Expression(node.toString());		// "value" in quotes
	}


	@Override
	public Expression caseMkBasicExpression(TCMkBasicExpression node, Environment arg)
	{
		if (node.type instanceof TCTokenType)
		{
			return new Expression(String.valueOf(node.arg.toString().hashCode()));	// raw token value
		}
		else if (node.type instanceof TCIntegerType)
		{
			return node.arg.apply(this, arg);
		}

		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	@Override
	public Expression caseMkTypeExpression(TCMkTypeExpression node, Environment env)
	{
		Expression mkexp = new Expression(new Text("mk_" + node.typename));

		for (TCExpression arg: node.args)
		{
			mkexp.add(arg.apply(this, env));
		}

		return mkexp;
	}

	@Override
	public Expression caseIsExpression(TCIsExpression node, Environment env)
	{
		if (node.basictype != null)
		{
			TCNumericType numeric = node.test.getType().getNumeric();
			boolean isNumeric = numeric != null;
			boolean isFloat = isNumeric && numeric.getWeight() > 2;
			boolean isBool = node.test.getType() instanceof TCBooleanType;

			if (node.basictype instanceof TCNaturalType)
			{
				Expression test = node.test.apply(this, env);

				if (isFloat)
				{
					return new And(
						new EQ(new Expression("to_int", test), test),
						new GE(test, new Text("0")));
				}
				else if (isNumeric)
				{
					return new GE(test, new Text("0"));
				}
				else
				{
					return new Expression("false");
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
				else if (isNumeric)
				{
					return new GT(test, new Text("0"));
				}
				else
				{
					return new Expression("false");
				}
			}
			else if (node.basictype instanceof TCRealType)
			{
				return new Expression(Boolean.toString(isFloat));
			}
			else if (node.basictype instanceof TCBooleanType)
			{
				if (node.test instanceof TCApplyExpression)		// pre_ or post_
				{
					TCApplyExpression apply = (TCApplyExpression)node.test;

					if (apply.type instanceof TCFunctionType)
					{
						TCFunctionType ftype = (TCFunctionType)apply.type;
						return new Expression(Boolean.toString(
							ftype.result instanceof TCBooleanType && !ftype.partial));
					}
				}
				
				return new Expression(Boolean.toString(isBool));
			}
		}
		
		throw new UnsupportedOperationException("Cannot convert: " + node);
	}

	@Override
	public Expression caseUndefinedExpression(TCUndefinedExpression node, Environment arg)
	{
		QualifiedSort qsort = node.getType().apply(new TypeToSMT("?"), arg);
		return new Expression("undefined" + qsort.sort);
	}

	@Override
	public Expression casePreOpExpression(TCPreOpExpression node, Environment arg)
	{
		return node.expression.apply(this, arg);
	}

	@Override
	public Expression casePostOpExpression(TCPostOpExpression node, Environment arg)
	{
		return node.postexpression.apply(this, arg);
	}

	/**
	 * Supporting methods.
	 */

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
