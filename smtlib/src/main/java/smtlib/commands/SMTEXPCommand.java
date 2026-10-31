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

package smtlib.commands;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.analyses.TCPlugin;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.POType;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import smtlib.SMTLIB;
import smtlib.ast.Script;
import smtlib.solvers.Solver;

public class SMTEXPCommand extends AnalysisCommand
{
	private final static String CMD = "smtexp [-<solver>] <expression>";
	private final static String SHORT = CMD;
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = SHORT + " - SMT solver testing";

	public SMTEXPCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("smtexp"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	class DummyObligation extends ProofObligation
	{
		public DummyObligation(String expression, Environment env) throws Exception
		{
			super(LexLocation.ANY, POType.THEOREM, new POContextStack()
			{
				public String getName()
				{
					return "smtexp";
				}
			});

			String defname = Interpreter.getInstance().getDefaultName();

			LexTokenReader ltr = new LexTokenReader(expression, Dialect.VDM_SL, LexLocation.ANY);
			ExpressionReader er = new ExpressionReader(ltr);
			er.setCurrentModule(defname);
			ASTExpression ast = er.readExpression();
			TCExpression exp = ClassMapper.getInstance(TCNode.MAPPINGS).convertLocal(ast);

			TypeChecker.clearErrors();
			exp.typeCheck(env, null, NameScope.NAMES, null);

			if (TypeChecker.getErrorCount() > 0)
			{
				for (VDMError err: TypeChecker.getErrors())
				{
					Console.out.println(err.toString());
				}

				throw new Exception("Expression has type errors");
			}

			setCheckedExpression(exp);
			source = exp.toString() + "\n";
		}
	}

	@Override
	public String run(String line)
	{
		if (Settings.dialect != Dialect.VDM_SL)
		{
			return "Only available for VDM-SL";
		}

		String type = null;
		String expression = null;

		Pattern p = Pattern.compile("^smtexp\\s+(-\\w+)?\\s*(.*)$");
		Matcher m = p.matcher(line);

		if (m.matches() && m.groupCount() == 2)
		{
			type = m.group(1);

			if (type == null)
			{
				type = Solver.DEFAULT_SOLVER;
			}
			else
			{
				type = type.substring(1);
			}

			expression = m.group(2);
		}
		else
		{
			return USAGE;
		}

		try
		{
			TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
			Environment env = tc.getGlobalEnvironment();
			SMTLIB smtlib = new SMTLIB(env, Solver.getInstance(type), true);

			ProofObligation po = new DummyObligation(expression, env);
			Script script = smtlib.generate(po);

			long before = System.currentTimeMillis();
			smtlib.runSolver(script, po);
			double duration = (double)(System.currentTimeMillis() - before)/1000;
			Console.out.print("PO #" + po.number + ", " + po.status.toString().toUpperCase());

			if (po.qualifier != null)
			{
				Console.out.printf(" %s", po.qualifier);
			}

			Console.out.println(" in " + duration + "s");

			if (po.getExplanation() != null)
			{
				Console.out.println(po.toTitle());
				Console.out.println(po.getExplanation());
			}
			else if (po.status == POStatus.FAILED)
			{
				Console.out.println(po.toTitle());
				Console.out.println(po.source);
			}

			return null;
		}
		catch (Exception e)
		{
			return "Failed to execute solver: " + e.getMessage();
		}
	}
}
