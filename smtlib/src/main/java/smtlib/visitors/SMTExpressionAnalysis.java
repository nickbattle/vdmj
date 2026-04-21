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

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.expressions.TCAbsoluteExpression;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCCasesExpression;
import com.fujitsu.vdmj.tc.expressions.TCElementsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCFloorExpression;
import com.fujitsu.vdmj.tc.expressions.TCInSetExpression;
import com.fujitsu.vdmj.tc.expressions.TCIndicesExpression;
import com.fujitsu.vdmj.tc.expressions.TCMapDomainExpression;
import com.fujitsu.vdmj.tc.expressions.TCUndefinedExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

import smtlib.ast.AssertCommand;
import smtlib.ast.Bracketed;
import smtlib.ast.Command;
import smtlib.ast.Comment;
import smtlib.ast.DeclareConst;
import smtlib.ast.DeclareFun;
import smtlib.ast.DefineFun;
import smtlib.ast.Expression;
import smtlib.ast.Script;
import smtlib.ast.Sort;
import smtlib.ast.Text;
import smtlib.ast.Undefined;

public class SMTExpressionAnalysis extends TCLeafExpressionVisitor<Command, Script, Environment>
{
	public SMTExpressionAnalysis()
	{
		visitorSet = new TCVisitorSet<Command, Script, Environment>()
		{
			@Override
			protected void setVisitors()
			{
				expressionVisitor = SMTExpressionAnalysis.this;
				patternVisitor = new SMTPatternAnalysis(this);
				multiBindVisitor = new SMTMultiBindAnalysis(this);
				typeVisitor = new SMTTypeAnalysis();
			}

			@Override
			protected Script newCollection()
			{
				return SMTExpressionAnalysis.this.newCollection();
			}
			
		};
	}

	@Override
	public Script caseExpression(TCExpression node, Environment env)
	{
		return newCollection();	// Nothing to add
	}

	@Override
	public Script caseAbsoluteExpression(TCAbsoluteExpression node, Environment env)
	{
		// abs is provided by one of the theories
		return super.caseAbsoluteExpression(node, env);
	}

	@Override
	public Script caseFloorExpression(TCFloorExpression node, Environment env)
	{
		Script all = super.caseFloorExpression(node, env);

		all.add(new Comment("Definition for floor", true));

		all.add(new DefineFun("floor",
			new Bracketed(new Bracketed(new Text("x"), new Sort("Real"))),
			new Sort("Int"),
			new Expression("to_int", "x")));

		return all;
	}

	@Override
	public Script caseApplyExpression(TCApplyExpression node, Environment env)
	{
		Script all = super.caseApplyExpression(node, env);
		TCDefinition root = findApplyDefinition(node, env);

		if (root instanceof TCExplicitFunctionDefinition)
		{
			TCExplicitFunctionDefinition def = (TCExplicitFunctionDefinition)root;

			if (def.name.isReserved())
			{
				return all;
			}
			else if (def.paramPatternList.size() == 1)
			{
				TCPatternList patterns = def.paramPatternList.get(0);
				Bracketed params = new Bracketed();		// eg. (x Int)
				Bracketed psorts = new Bracketed();		// eg. (Int)
				Expression qualifiers = null;
				Expression call = new Expression(def.name.getName());	// eg. (g a b c)

				for (int i=0; i < patterns.size(); i++)
				{
					TCPattern p = patterns.get(i);
					TCType t = def.type.parameters.get(i);
					
					if (p instanceof TCIdentifierPattern)
					{
						String name = p.toString();
						QualifiedSort qsort = t.apply(new TypeToSMTConverter(name), env);
						params.add(new Bracketed(new Text(name), qsort.sort));
						psorts.add(qsort.sort);
						call.add(new Text(name));

						if (qsort.qualifier != null)
						{
							if (qualifiers == null)
							{
								qualifiers = qsort.qualifier;
							}
							else
							{
								qualifiers = new Expression("and", qsort.qualifier, qualifiers);
							}
						}
					}
					else
					{
						throw new UnsupportedOperationException("Cannot convert complex pattern: " + p);
					}
				}

				Expression body = def.body.apply(new ExpressionToSMTConverter(), env);
				QualifiedSort qsort = def.type.result.apply(new TypeToSMTConverter("RESULT"), env);

				all.add(new Comment("Definitions for function " + def.name, true));

				// (declare-fun g (Int) Int)
				all.add(new DeclareFun(def.name.getName(), psorts, qsort.sort));

				if (!psorts.isEmpty())
				{
					if (qualifiers == null)
					{
						// (assert (forall ((<params>)) (= (g <params>) (<body>)))
						all.add(new AssertCommand(new Expression(
							new Text("forall"), params,
								new Expression("=", call, body))));
					}
					else
					{
						// (assert (forall ((<params>)) (=> (<qualifiers>) (= (g <params>) (<body>)))))
						all.add(new AssertCommand(new Expression(
							new Text("forall"), params,
								new Expression("=>",
									qualifiers,
									new Expression("=", call, body)))));
					}
				}
				else	// Function is a constant, like g() == 123;
				{
					// (assert (= g (<body>)))
					all.add(new AssertCommand(new Expression(
						new Expression("=", call, body))));
				}
			}
		}
		else if (root instanceof TCLocalDefinition)
		{
			TCLocalDefinition local = (TCLocalDefinition)root;

			if (local.type.isMap(local.location))
			{
				TCMapType mtype = local.type.getMap();
				String mapname = local.name.getName();

				QualifiedSort domsort = mtype.to.apply(new TypeToSMTConverter("k"), env);
				Expression key = node.args.get(0).apply(new ExpressionToSMTConverter(), env);

				all.add(new AssertCommand(
					new Expression("=>",
						new Expression(new Text("set.member"), key, new Text("dom_" + mapname)),
						domsort.qualifier)));
			}
		}

		return all;
	}

	@Override
	public Script caseCasesExpression(TCCasesExpression node, Environment env)
	{
		Script all = super.caseCasesExpression(node, env);

		if (node.others == null)
		{
			all.add(new Undefined());
		}
		
		return all;
	}

	@Override
	public Script caseIndicesExpression(TCIndicesExpression node, Environment env)
	{
		Script all = super.caseIndicesExpression(node, env);
		QualifiedSort seqof = node.exp.getType().apply(new TypeToSMTConverter("?"), env);
		Sort setof = new Sort(new Text("Set"), new Text("Int"));

		all.add(new DeclareFun("vdm.inds", new Bracketed(seqof.sort), setof));
		// forall ((s (Seq Int)) (i Int)) (= (set.member i (inds s)) (and (>= i 0) (< i (seq.len s)))))
		all.add(new AssertCommand(
			new Expression("forall",
				new Bracketed(
					new Expression(new Text("s"), seqof.sort),
					new Expression("i", "Int")),
				new Expression("=",
					new Expression(new Text("set.member"),
						new Text("i"),
						new Expression("vdm.inds", "s")),
					new Expression(new Text("and"),
						new Expression(">=", "i", "0"),
						new Expression(new Text("<"),
							new Text("i"),
							new Expression("seq.len", "s")))
				))));

		return all;
	}

	@Override
	public Script caseElementsExpression(TCElementsExpression node, Environment env)
	{
		Script all = super.caseElementsExpression(node, env);
		QualifiedSort seqof = node.exp.getType().apply(new TypeToSMTConverter("?"), env);
		Sort setof = new Sort(new Text("Set"), seqof.sort.get(1));

		all.add(new DeclareFun("vdm.elems", new Bracketed(seqof.sort), setof));
		// (forall ((s (Seq Int)) (x Int))
		//   (= (set.member x (elems s))
		//      (exists ((i Int)) (and (>= i 0) (< i (seq.len s)) (= (seq.nth s i) x)))))
		all.add(new AssertCommand(
			new Expression("forall",
				new Bracketed(
					new Expression(new Text("s"), seqof.sort),
					new Expression(new Text("x"), seqof.sort.get(1))),
				new Expression("=",
					new Expression(new Text("set.member"),
						new Text("x"),
						new Expression("vdm.elems", "s")),
					new Expression(new Text("exists"),
						new Bracketed(new Expression("i", "Int")),
						new Expression("and",
							new Expression(">=", "i", "0"),
							new Expression(new Text("<"),
								new Text("i"),
								new Expression("seq.len", "s")),
							new Expression(new Text("="),
								new Expression("seq.nth", "s", "i"),
								new Text("x")
							)))))));

		return all;
	}

	@Override
	public Script caseInSetExpression(TCInSetExpression node, Environment env)
	{
		Script script = newCollection();

		if (node.left instanceof TCVariableExpression &&	// k in set dom m
			node.right instanceof TCMapDomainExpression)
		{
			TCVariableExpression key = (TCVariableExpression)node.left;
			TCMapDomainExpression dom = (TCMapDomainExpression)node.right;

			if (dom.exp instanceof TCVariableExpression)
			{
				TCVariableExpression map = (TCVariableExpression)dom.exp;
				String keyname = key.name.getName();
				String mapname = map.name.getName();

				TCMapType mtype = map.getType().getMap();
				QualifiedSort domsort = mtype.from.apply(new TypeToSMTConverter(keyname), env);
				QualifiedSort rngsort = mtype.to.apply(new TypeToSMTConverter(keyname), env);

				// Add a "use" of the map, to force the solver to use it
				script.add(new DeclareConst(keyname, domsort.sort));
				script.add(new DeclareConst("$v", rngsort.sort));
				// (assert (= v (select m k)))
				script.add(new AssertCommand(new Expression("=", new Text("$v"),
					new Expression("select", mapname, keyname))));
			}
			else
			{
				throw new UnsupportedOperationException("Cannot convert: " + node);
			}
		}

		return script;
	}

	@Override
	public Script caseUndefinedExpression(TCUndefinedExpression node, Environment env)
	{
		return new Script(new Undefined());
	}

	@Override
	protected Script newCollection()
	{
		return new Script();
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
}
