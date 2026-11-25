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
import java.util.Vector;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCBinaryExpression;
import com.fujitsu.vdmj.tc.expressions.TCBooleanLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCFieldExpression;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.expressions.TCHeadExpression;
import com.fujitsu.vdmj.tc.expressions.TCIfExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
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
import com.fujitsu.vdmj.tc.patterns.TCRecordPattern;
import com.fujitsu.vdmj.tc.patterns.TCTuplePattern;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Pair;

import smtlib.ast.And;
import smtlib.ast.Bracketed;
import smtlib.ast.EQ;
import smtlib.ast.Exists;
import smtlib.ast.Expression;
import smtlib.ast.ExpressionSet;
import smtlib.ast.ForAll;
import smtlib.ast.GE;
import smtlib.ast.GT;
import smtlib.ast.ITE;
import smtlib.ast.LE;
import smtlib.ast.LT;
import smtlib.ast.Let;
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
	public SMTPair caseFieldExpression(TCFieldExpression node, Environment env)
	{
		SMTPair object = node.object.apply(this, env);
		return new SMTPair(new Expression(new Text(node.field.getName()), object.value), object.deltas);
	}

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
	public SMTPair caseApplyExpression(TCApplyExpression node, Environment env)
	{
		TCDefinition root = null;

		if (node.root instanceof TCVariableExpression)	// Must be s(i)
		{
			TCVariableExpression var = (TCVariableExpression)node.root;
			root = env.findName(var.name, NameScope.NAMESANDSTATE);
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
			TCExplicitFunctionDefinition def = (TCExplicitFunctionDefinition)root;

			if (def.paramPatternList.size() == 1)	// Not curried
			{
				Expression call = new Expression(def.name.getName());
				ExpressionSet deltas = new ExpressionSet();

				for (TCExpression arg: node.args)
				{
					SMTPair pair = arg.apply(this, env);
					call.add(pair.value);
					deltas.addAll(pair.deltas);
				}

				// Add a delta condition to call <name>_defined(args), which is
				// created from the SMTPair's delta when the function is translated.

				deltas.add(new Expression(def.name.getName() + "_defined"));

				if (def.predef != null)
				{
					Expression precall = new Expression();
					precall.addAll(call);
					precall.set(0, new Text(def.predef.name.getName()));
					deltas.add(precall);
				}

				if (def.postdef != null)
				{
					Expression postcall = new Expression();
					postcall.addAll(call);
					postcall.set(0, new Text(def.postdef.name.getName()));
					postcall.add(call);
					deltas.add(postcall);
				}

				return new SMTPair(call, deltas);
			}
		}
		else if (root instanceof TCImplicitFunctionDefinition)
		{
			TCImplicitFunctionDefinition def = (TCImplicitFunctionDefinition)root;
			Expression call = new Expression(def.name.getName());
			ExpressionSet deltas = new ExpressionSet();

			for (TCExpression arg: node.args)
			{
				SMTPair pair = arg.apply(this, env);
				call.add(pair.value);
				deltas.addAll(pair.deltas);
			}

			// Add a delta condition to call <name>_defined(args), which is
			// created from the SMTPair's delta when the function is translated.

			deltas.add(new Expression(def.name.getName() + "_defined"));

			if (def.predef != null)
			{
				Expression precall = new Expression();
				precall.addAll(call);
				precall.set(0, new Text(def.predef.name.getName()));
				deltas.add(precall);
			}

			if (def.postdef != null)
			{
				Expression postcall = new Expression();
				postcall.addAll(call);
				postcall.set(0, new Text(def.postdef.name.getName()));
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

	@Override
	public SMTPair caseBinaryExpression(TCBinaryExpression node, Environment env)
	{
		SMTPair left = node.left.apply(this, env);
		SMTPair right = node.right.apply(this, env);

		return new SMTPair(
			new Expression(node.op.toString(), left.value, right.value),
			new ExpressionSet(left.deltas, right.deltas));
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

		if (ifExp.deltas != null)
		{
			delta.addAll(ifExp.deltas);
		}

		if (!thenExp.deltas.isEmpty() || !elseExp.deltas.isEmpty())
		{
			delta.add(new ITE(ifExp.value,
				thenExp.deltas.andOf(),
				elseExp.deltas.andOf()));
		}

		return new SMTPair(value, delta);
	}

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

	@Override
	public SMTPair caseLetDefExpression(TCLetDefExpression node, Environment env)
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
}
