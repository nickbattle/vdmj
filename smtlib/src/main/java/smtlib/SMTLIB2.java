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

package smtlib;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.typechecker.Environment;

import smtlib.ast.And;
import smtlib.ast.AssertCommand;
import smtlib.ast.Bracketed;
import smtlib.ast.Comment;
import smtlib.ast.DeclareConst;
import smtlib.ast.Expression;
import smtlib.ast.Not;
import smtlib.ast.Script;
import smtlib.ast.Source;
import smtlib.solvers.Solver;
import smtlib.visitors.DefinitionExpander;
import smtlib.visitors.ExpressionToSMTPair;
import smtlib.visitors.Globals;
import smtlib.visitors.LocalNames;
import smtlib.visitors.SMTPair;

public class SMTLIB2 extends SMTLIB
{
	public SMTLIB2(Environment env, Solver solver, boolean verbose)
	{
		super(env, solver, verbose);
	}

	@Override
	protected Script topLevelDefinitions(ProofObligation po)
	{
		TCExpression tcexp = po.getCheckedExpression();
		Script script = new Script();
		LocalNames.reset();
		Globals.getInstance().init();
		Environment smtenv = Globals.getInstance().getEnvironment(env);		// Includes invs
		DefinitionExpander.init(smtenv);

		try
		{
			

			if (tcexp instanceof TCForAllExpression)
			{
				TCForAllExpression node = (TCForAllExpression)tcexp;

				SMTPair forall = node.apply(new ExpressionToSMTPair(), smtenv);
				script.addAll(Globals.getInstance().globalList());

				Bracketed binds = (Bracketed)forall.value.get(1);
				script.add(new Comment("\nTop level forall variables"));

				// Add declare-consts for the forall bindings, to get a model
				for (Source bind: binds)
				{
					Expression pair = (Expression)bind;
					script.add(new DeclareConst(pair.get(0), pair.get(1)));
				}

				script.add(new Comment("\nObligation satisfiable?"));
				Expression body = (Expression) forall.value.get(2);

				if (forall.deltas.isEmpty())
				{
					script.add(new AssertCommand(new Not(body)));
				}
				else
				{
					script.add(new AssertCommand(new Not(new And(forall.deltas.andOf(), body))));
				}
			}
			else if (tcexp instanceof TCExistsExpression)
			{
				TCExistsExpression node = (TCExistsExpression)tcexp;

				SMTPair exists = node.apply(new ExpressionToSMTPair(), smtenv);
				script.addAll(Globals.getInstance().globalList());

				Bracketed binds = (Bracketed)exists.value.get(1);
				script.add(new Comment("\nTop level exists variables"));

				// Add define-consts for the exists bindings, to get a model
				for (Source bind: binds)
				{
					Expression pair = (Expression)bind;
					script.add(new DeclareConst(pair.get(0), pair.get(1)));
				}

				script.add(new Comment("\nObligation satisfiable?"));
				Expression body = (Expression) exists.value.get(2);

				if (exists.deltas.isEmpty())
				{
					script.add(new AssertCommand(body));
				}
				else
				{
					script.add(new AssertCommand(new And(exists.deltas.andOf(), body)));
				}
			}
			else if (tcexp.getType() instanceof TCBooleanType)	// eg. original inv_T(args)
			{
				script.add(new Comment("\nObligation satisfiable?"));
				SMTPair apply = tcexp.apply(new ExpressionToSMTPair(), smtenv);

				if (apply.deltas.isEmpty())
				{
					script.add(new AssertCommand(new Not(apply.value)));
				}
				else
				{
					script.add(new AssertCommand(new Not(new And(apply.deltas.andOf(), apply.value))));
				}
			}
			else
			{
				throw new UnsupportedOperationException("Obligation not a boolean: " + tcexp);
			}
		}
		catch (Exception e)
		{
			if (verbose)
			{
				if (tcexp != null)
				{
					Console.out.println("Expanded: " + tcexp.toString().replace("\n", " "));
					Console.out.println("----");
				}

				printScript(script);
			}

			throw e;
		}

		return script;
	}
}
