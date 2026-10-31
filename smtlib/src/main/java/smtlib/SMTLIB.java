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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.expressions.TCAndExpression;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionSet;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import smtlib.ast.AssertCommand;
import smtlib.ast.Bracketed;
import smtlib.ast.CheckSat;
import smtlib.ast.Comment;
import smtlib.ast.DeclareConst;
import smtlib.ast.Expression;
import smtlib.ast.GetModel;
import smtlib.ast.Not;
import smtlib.ast.PopCommand;
import smtlib.ast.PushCommand;
import smtlib.ast.Script;
import smtlib.ast.SetOption;
import smtlib.ast.Source;
import smtlib.solvers.Solver;
import smtlib.visitors.ASTHelper;
import smtlib.visitors.DefinitionExpander;
import smtlib.visitors.ExpressionExpander;
import smtlib.visitors.ExpressionToSMT;
import smtlib.visitors.Globals;
import smtlib.visitors.LocalNames;
import smtlib.visitors.Utils;

public class SMTLIB
{
	private final Environment env;
	private final Solver solver;
	private final boolean verbose;

	public SMTLIB(Environment env, Solver solver, boolean verbose)
	{
		this.env = env;
		this.solver = solver;
		this.verbose = verbose;
	}

	public Script generate(ProofObligation po)
	{
		Script script = new Script("Proof Obligation #" + po.number);
		script.add(new SetOption(":timeout", "5000"));
		script.add(new Comment("Using solver " + solver.getName()));
		script.add(new PushCommand());
		script.add(new Comment("\nDefinitions for PO #" + po.number));
		script.addAll(topLevelDefinitions(po));
		script.add(new Comment("\nCheck result"));
		script.add(new CheckSat());
		script.add(new GetModel());
		script.add(new PopCommand());

		return script;
	}

	private Script topLevelDefinitions(ProofObligation po)
	{
		TCExpression tcexp = po.getCheckedExpression();
		Script script = new Script();
		LocalNames.reset();
		Globals.getInstance().reset();
		DefinitionExpander.init(env);
		TCExpression expanded = null;

		try
		{
			TCExpressionSet constraints = new TCExpressionSet();
			expanded = tcexp.apply(new ExpressionExpander(env), constraints);

			if (!constraints.isEmpty())
			{
				expanded = new TCAndExpression(Utils.andOf(constraints), ASTHelper.AND, expanded);
			}

			TypeChecker.clearErrors();
			expanded.typeCheck(env, null, NameScope.NAMES, null);
			script.add(new Comment("Expanded PO: " + Utils.expandOneLine(expanded)));

			for (VDMError err: TypeChecker.getErrors())
			{
				script.add(new Comment(err.message));	// Should be none!
			}

			script.addAll(Globals.getInstance().globalList());

			if (expanded instanceof TCForAllExpression)
			{
				TCForAllExpression node = (TCForAllExpression)expanded;

				script.add(new Comment("\nTop level forall variables"));
				Expression forall = node.apply(new ExpressionToSMT(), env);
				Bracketed binds = (Bracketed)forall.get(1);

				// Add declare-consts for the forall bindings, to get a model
				for (Source bind: binds)
				{
					Expression pair = (Expression)bind;
					script.add(new DeclareConst(pair.get(0), pair.get(1)));
				}

				script.add(new Comment("\nObligation satisfiable?"));
				Expression body = (Expression) forall.get(2);
				script.add(new AssertCommand(new Not(body)));
			}
			else if (expanded instanceof TCExistsExpression)
			{
				TCExistsExpression node = (TCExistsExpression)expanded;

				Expression exists = node.apply(new ExpressionToSMT(), env);
				Bracketed binds = (Bracketed)exists.get(1);
				script.add(new Comment("\nTop level exists variables"));

				// Add define-consts for the exists bindings, to get a model
				for (Source bind: binds)
				{
					Expression pair = (Expression)bind;
					script.add(new DeclareConst(pair.get(0), pair.get(1)));
				}

				script.add(new Comment("\nObligation satisfiable?"));
				Expression body = (Expression) exists.get(2);
				script.add(new AssertCommand(body));
			}
			else if (expanded.getType() instanceof TCBooleanType)	// eg. inv_T(args)
			{
				script.add(new Comment("\nObligation satisfiable?"));
				Expression apply = expanded.apply(new ExpressionToSMT(), env);
				script.add(new AssertCommand(new Not(apply)));
			}
			else
			{
				throw new UnsupportedOperationException("Obligation not a boolean: " + expanded);
			}
		}
		catch (Exception e)
		{
			if (verbose)
			{
				if (expanded != null)
				{
					Console.out.println("Expanded: " + expanded.toString().replace("\n", " "));
					Console.out.println("----");
				}

				printScript(script);
			}

			throw e;
		}

		return script;
	}

	public void runSolver(Script script, ProofObligation po) throws IOException
	{
		Process p = solver.getProcess().start();

		if (verbose) printScript(script);
		
		OutputStream os = p.getOutputStream();
		os.write(script.toFormat(0).getBytes());
		os.flush();

		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = br.readLine();
		String error = null;

		if (line == null)	// Strange solver behaviour?
		{
			BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			line = err.readLine();

			while (line != null)
			{
				Console.err.println(line);
				line = err.readLine();
			}

			err.close();
			br.close();
			os.close();
			throw new IOException("No result from solver?");
		}

		switch (line)
		{
			case "sat":
				po.setStatus(po.isExistential() ? POStatus.PROVED : POStatus.FAILED);
				parseModel(br, po);
				break;

			case "unsat":
				if (po.isExistential())
				{
					po.setStatus(POStatus.FAILED);
					po.setQualifier("unsatisfiable");
				}
				else
				{
					po.setStatus(POStatus.PROVED);
				}
				break;

			case "unknown":
				po.setStatus(POStatus.MAYBE);
				break;

			default:
				error = line;
				break;
		}
		
		br.close();
		os.close();
		
		if (error != null)
		{
			throw new IOException(error);
		}
	}

	private void parseModel(BufferedReader br, ProofObligation po) throws IOException
	{
		Context ctxt = new Context(po.location, "counterexample",
			new Context(LexLocation.ANY, "outer", null));
		
		String explanation = solver.getExplanation(br, po, ctxt);

		if (explanation != null)
		{
			po.setExplanation(explanation);
		}

		if (!ctxt.isEmpty())
		{
			if (po.isExistential())
			{
				po.setWitness(ctxt);
			}
			else
			{
				po.setCounterexample(ctxt);
			}
		}
	}

	private void printScript(Script script)
	{
		int n = 1;

		for (Object line: script.toFormat(0).lines().toArray())
		{
			Console.out.printf("%3d: %s\n", n++, line);
		}

		Console.out.println("----");
	}
}
