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

package smtlib.solvers;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.GetResource;
import com.fujitsu.vdmj.values.SeqValue;

import smtlib.parser.Bracket;
import smtlib.parser.SMTLIBReader;

abstract public class Solver implements VDMSolver
{
	public static final String DEFAULT_SOLVER = "z3";
	private static Solver INSTANCE = null;
	private static Map<String, Solver> solvers = null;

	public static Solver getInstance(String type)
	{
		if (solvers == null)
		{
			init();
		}

		INSTANCE = solvers.get(type);

		if (INSTANCE == null)
		{
			throw new UnsupportedOperationException("Unknown solver type: " + type);
		}

		return INSTANCE;
	}

	public static Solver getInstance()
	{
		if (INSTANCE != null)
		{
			return INSTANCE;
		}

		return getInstance(DEFAULT_SOLVER);
	}

	/**
	 * Create solvers from the "smt.solvers" resource.
	 */
	private static void init()
	{
		try
		{
			solvers = new HashMap<String, Solver>();

			for (String classname: GetResource.readResource("smt.solvers"))
			{
				try
				{
					Class<?> clazz = Class.forName(classname);
					Constructor<?> ctor = clazz.getConstructor();
					Solver solver = (Solver)ctor.newInstance();

					solvers.put(solver.getName(), solver);
				}
				catch (Exception e)
				{
					throw new UnsupportedOperationException("Cannot instantiate solver " + classname + ": " + e.getMessage());
				}
			}
		}
		catch (Exception e)
		{
			throw new UnsupportedOperationException("Cannot instantiate solvers: " + e.getMessage());
		}
	}

	/**
	 * The solver name as recognised by the command line parser
	 */
	public abstract String getName();

	/**
	 * Get a process builder to start the solver in standard-input mode
	 */
	public abstract ProcessBuilder getProcess();

	/**
	 * Get model name and VDM value strings after a "sat" result.
	 */
	public String[] getModelNameValue(Bracket model)
	{
		String name = model.get(1).toString();

		if (name.startsWith("undefined"))
		{
			return null;	// Ignore this model item
		}

		// "(define-fun a () Real (/ 1 2))"
		// "(define-fun t () Int 0)"

		return new String[]
			{
				getVDMName(model.gets(1)),
				getVDMValue(model.get(4))
			};
	}

	/**
	 * Turn a solver model name into a VDM name.
	 */
	protected String getVDMName(String smtvalue)
	{
		return smtvalue;
	}

	/**
	 * Turn a solver model value into a VDM value. Passed a String or a Bracket
	 */
	protected String getVDMValue(Object smtValue)
	{
		if (smtValue instanceof Bracket)
		{
			Bracket bracket = (Bracket) smtValue;
			String first = bracket.firstElement().toString();

			switch (first)
			{
				case "as":
					if (bracket.get(1).equals("seq.empty"))
					{
						return "[]";
					}
					else if (bracket.get(1).equals("set.empty"))
					{
						return "{}";
					}
					else
					{
						return smtValue.toString();
					}

				case "seq.unit":
					return "[" + getVDMValue(bracket.get(1)) + "]";

				case "set.singleton":
					return "{" + getVDMValue(bracket.get(1)) + "}";

				case "seq.++":
				{
					String result = "";
					String sep = "";

					for (int i=1; i < bracket.size(); i++)
					{
						result = result + sep + getVDMValue(bracket.get(i));
						sep = " ^ ";
					}

					return result;
				}

				case "set.union":
				{
					String result = "";
					String sep = "";

					for (int i=1; i < bracket.size(); i++)
					{
						result = result + sep + getVDMValue(bracket.get(i));
						sep = " union ";
					}

					return result;
				}

				case "-":
					return "-" + getVDMValue(bracket.get(1));

				default:
					if (first.startsWith("mk_"))
					{
						StringBuilder sb = new StringBuilder();
						sb.append(first);
						sb.append("(");
						String sep = "";

						for (int i=1; i < bracket.size(); i++)
						{
							sb.append(sep);
							sb.append(getVDMValue(bracket.get(i)));
							sep = ", ";
						}

						sb.append(")");
						return sb.toString();
					}
					else
					{
						return smtValue.toString();
					}
			}
		}
		else
		{
			return smtValue.toString();
		}
	}

	/**
	 * Parse the model returned by the solver, and update a Context and return a text explanation.
	 */
	public String getExplanation(BufferedReader br, ProofObligation po, Context ctxt)
		throws IOException
	{
		StringBuilder explanation = null;
		SMTLIBReader reader = new SMTLIBReader(br);
		Bracket model = reader.readBracket();

		for (int i=0; i < model.size(); i++)
		{
			// "(define-fun a () Real (/ 1 2))"
			// "(define-fun t () Int 0)"

			Bracket definition = model.getb(i);
			String[] nvp = getModelNameValue(definition);

			if (nvp == null)
			{
				continue;	// Ignored value
			}

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

			explanation.append(nvp[0]);
			explanation.append(" = ");
			explanation.append(nvp[1]);
			explanation.append("\n");

			ctxt.put(new TCNameToken(po.location, po.location.module, nvp[0]), new SeqValue(nvp[1]));
		}

		return explanation == null ? null : explanation.toString();
	}
}
