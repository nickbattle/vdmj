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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.values.SeqValue;

import smtlib.ast.AssertCommand;
import smtlib.ast.Bracketed;
import smtlib.ast.CheckSat;
import smtlib.ast.Command;
import smtlib.ast.Comment;
import smtlib.ast.DeclareConst;
import smtlib.ast.Expression;
import smtlib.ast.GetModel;
import smtlib.ast.Implies;
import smtlib.ast.Not;
import smtlib.ast.PopCommand;
import smtlib.ast.PushCommand;
import smtlib.ast.Script;
import smtlib.ast.Source;
import smtlib.parser.Bracket;
import smtlib.parser.SMTLIBReader;
import smtlib.visitors.ExpressionToSMTConverter;
import smtlib.visitors.SMTExpressionAnalysis;

public class SMTLIB
{
	public static final String DEFAULT_SOLVER = "-cvc5";

	private final Environment env;
	private final String solver;

	public SMTLIB(Environment env, String solver)
	{
		this.env = env;
		this.solver = solver;
	}

	public Script generate(ProofObligation po)
	{
		Script script = new Script("Proof Obligation #" + po.number);
		script.add(new Comment("Using solver " + solver));
		script.add(new PushCommand());
		script.add(new Comment("Definitions for PO #" + po.number, true));
		script.addAll(topLevelDefinitions(po));
		script.add(new Comment("Check result", true));
		script.add(new CheckSat());
		script.add(new GetModel());
		script.add(new PopCommand());

		return script;
	}

	private Script topLevelDefinitions(ProofObligation po)
	{
		TCExpression tcexp = po.getCheckedExpression();
		Script script = new Script();

		if (tcexp instanceof TCForAllExpression)
		{
			TCForAllExpression node = (TCForAllExpression)tcexp;
			FlatEnvironment locals = new FlatEnvironment(new TCDefinitionList(), env);

			Expression forall = node.apply(new ExpressionToSMTConverter(), locals);
			Bracketed binds = (Bracketed)forall.get(1);

			// Add define-consts for the forall bindings
			for (Source bind: binds)
			{
				Expression pair = (Expression)bind;
				script.add(new DeclareConst(pair.get(0), pair.get(1)));
			}

			// Extract any define-funcs we need for the obligation, which may use
			// the declare-consts above.
			script.addAll(tcexp.apply(new SMTExpressionAnalysis(), locals));
			removeDuplicates(script);

			// Add top level assertion if there are any qualifiers
			Expression body = (Expression)forall.get(2);

			if (body instanceof Implies)
			{
				script.add(new AssertCommand(body.get(1)));
				body = (Expression)body.get(2);
			}

			script.add(new Comment("Obligation satisfiable?", true));
			script.add(new AssertCommand(new Not(body)));
		}
		else if (tcexp instanceof TCExistsExpression)
		{
			TCExistsExpression node = (TCExistsExpression)tcexp;
			FlatEnvironment locals = new FlatEnvironment(new TCDefinitionList(), env);

			Expression exists = node.apply(new ExpressionToSMTConverter(), locals);
			Bracketed binds = (Bracketed)exists.get(1);

			// Add define-consts for the exists bindings
			for (Source bind: binds)
			{
				Expression pair = (Expression)bind;
				script.add(new DeclareConst(pair.get(0), pair.get(1)));
			}

			// Extract any define-funcs we need for the obligation, which may
			// use the declare-consts above.
			script.addAll(tcexp.apply(new SMTExpressionAnalysis(), locals));
			removeDuplicates(script);

			// Add top level assertion if there are any qualifiers
			Expression body = (Expression)exists.get(2);

			if (body instanceof Implies)
			{
				script.add(new AssertCommand(body.get(1)));
				body = (Expression)body.get(2);
			}

			script.add(new Comment("Obligation satisfiable?", true));
			script.add(new AssertCommand(body));
		}
		else
		{
			throw new UnsupportedOperationException("Unexpected top level PO expression");
		}

		return script;
	}

	private void removeDuplicates(Script script)
	{
		Set<String> done = new HashSet<String>();
		Iterator<Command> iter = script.iterator();

		while (iter.hasNext())
		{
			Command cmd = iter.next();

			if (cmd instanceof DeclareConst)
			{
				String name = cmd.get(1).toSource();	// declare-const name sort

				if (done.contains(name))
				{
					iter.remove();
				}
				else
				{
					done.add(name);
				}
			}
		}
	}

	public void runSolver(Script script, ProofObligation po) throws IOException
	{
		Process p = null;
		
		switch (solver)
		{
			case "-cvc5":
				p = new ProcessBuilder("cvc5", "--lang", "smt2", "--quiet", "--incremental", "--sets-ext").start();
				break;

			case "-z3":
				p = new ProcessBuilder("z3", "-in").start();
				break;

			default:
				throw new UnsupportedOperationException("Unknown solver: " + solver +". Try -cvc5 or -z3");
		}

		OutputStream os = p.getOutputStream();
		os.write(script.toSource().getBytes());
		os.flush();

		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = br.readLine();
		String error = null;

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
		StringBuilder explanation = null;
		Context ctxt =
			new Context(po.location, "counterexample",
				new Context(LexLocation.ANY, "outer", null));
		
		SMTLIBReader reader = new SMTLIBReader(br);
		Bracket model = reader.readBracket();

		for (int i=0; i < model.size(); i++)
		{
			// "(define-fun a () Real (/ 1 2))" or "((define-fun t () Int 0))"
			Bracket var = model.getb(i);
			String name  = var.gets(1);
			String value = var.gets(4);

			if (explanation == null)
			{
				explanation = new StringBuilder();
				explanation.append(po.source);

				if (po.isExistential())
				{
					explanation.append("---- Witness:\n");
				}
				else
				{
					explanation.append("---- Counterexample:\n");
				}
			}

			explanation.append(name);
			explanation.append(" = ");
			explanation.append(value);
			explanation.append("\n");

			ctxt.put(new TCNameToken(po.location, po.location.module, name), new SeqValue(value));
		}

		if (explanation != null)
		{
			po.setExplanation(explanation.toString());
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
}
