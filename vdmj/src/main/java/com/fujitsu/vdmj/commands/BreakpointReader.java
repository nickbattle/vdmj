/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.commands;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.in.expressions.INBinaryExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.Value;

public class BreakpointReader
{
	private final Interpreter interpreter;
	
	public BreakpointReader(Interpreter interpreter)
	{
		this.interpreter = interpreter;
	}
	
	private void println(String m)
	{
		Console.out.println(m);
	}

	public boolean doCommand(String line)
	{
		try
		{
			if (line.startsWith("break"))
			{
				return doBreak(line);
			}
			else if (line.startsWith("trace"))
			{
				return doTrace(line);
			}
			else if (line.startsWith("list"))
			{
				return doList(line);
			}
			if (line.startsWith("remove"))
			{
				return doRemove(line);
			}
			
			println("Illegal breakpoint command");
			return true;
		}
		catch (Exception e)
		{
			return doException(e);
		}
	}

	private boolean doException(Exception e)
	{
		while (e instanceof InvocationTargetException)
		{
			e = (Exception)e.getCause();
		}
		
		println("Exception: " + e.getMessage());
		return true;
	}

	public boolean doList(@SuppressWarnings("unused") String line)
	{
		Map<Integer, Breakpoint> map = interpreter.getBreakpoints();

		for (Entry<Integer, Breakpoint> entry: map.entrySet())
		{
			Breakpoint bp = entry.getValue();
			println(bp.toString());
			println(interpreter.getSourceLine(bp.location));
		}

		return true;
	}

	public boolean doRemove(String line)
	{
		String parts[] = line.split("\\s+");

		if (parts.length != 2)
		{
			println("Usage: remove <breakpoint#>");
			return true;
		}

		int bpno = Integer.parseInt(parts[1]);
		Breakpoint old = interpreter.clearBreakpoint(bpno);

		if (old != null)
		{
			println("Cleared " + old);
			println(interpreter.getSourceLine(old.location));
		}
		else
		{
			println("Breakpoint [" + bpno + "] not set");
		}

		return true;
	}

	public boolean doBreak(String line) throws Exception
	{
		Pattern p1 = Pattern.compile("^break ([\\w._/\\\\]++)?:?(\\d+) ?(.+)?$");
		Matcher m = p1.matcher(line);

		if (m.matches())
		{
			String g1 = m.group(1);
			File file = g1 == null ? null : new File(g1);
			setBreakpoint(file, Integer.parseInt(m.group(2)), m.group(3));
		}
		else
		{
			Pattern p2 = Pattern.compile("^break ([\\w`$%']+) ?(.+)?$");
			m = p2.matcher(line);

			if (m.matches())
			{
				setBreakpoint(m.group(1), m.group(2));
			}
			else
			{
	    		println("Usage: break [<file>:]<lineno> [<condition>]");
	    		println("   or: break <function/operation> [<condition>]");
			}
		}

		return true;
	}

	public boolean doTrace(String line) throws Exception
	{
		Pattern p1 = Pattern.compile("^trace ([\\w._/\\\\]++)?:?(\\d+) ?(.+)?$");
		Matcher m = p1.matcher(line);

		if (m.matches())
		{
			String g1 = m.group(1);
			File file = g1 == null ? null : new File(g1);
			setTracepoint(file, Integer.parseInt(m.group(2)), m.group(3));
		}
		else
		{
			Pattern p2 = Pattern.compile("^trace ([\\w`$%']+) ?(.+)?$");
			m = p2.matcher(line);

			if (m.matches())
			{
				setTracepoint(m.group(1), m.group(2));
			}
			else
			{
	    		println("Usage: trace [<file>:]<lineno> [<expression>]");
	    		println("   or: trace <function/operation> [<expression>]");
			}
		}

		return true;
	}
	
	/**
	 * Set a breakpoint at the given file and line with a condition.
	 *
	 * @param file The file name
	 * @param line The line number
	 * @param condition Any condition for the breakpoint, or null
	 * @throws Exception Problems parsing condition.
	 */
	private void setBreakpoint(File file, int line, String condition)
		throws Exception
	{
		if (file == null)
		{
			file = interpreter.getDefaultFile();
		}
		
		if (file == null || file.getPath().equals("?"))
		{
			Set<File> files = interpreter.getSourceFiles();
			
			if (files.size() > 1)
			{
				println("Assuming file " + file.getPath());
			}
			else if (files.isEmpty())
			{
				println("No files defined");
				return;
			}

			file = files.iterator().next();
		}

		INStatement stmt = interpreter.findStatement(file, line);

		if (stmt == null)
		{
			INExpression exp = interpreter.findExpression(file, line);

			if (exp == null)
			{
				println("No breakable expressions or statements at " + file + ":" + line);
			}
			else
			{
				Breakpoint old = interpreter.clearBreakpoint(exp.breakpoint.number);
				
				if (old != null)
				{
					println("Overwriting [" + old.number + "] " + old.location);
				}
				
				Breakpoint bp = interpreter.setBreakpoint(exp, condition);
				println("Created " + bp);
				println(interpreter.getSourceLine(bp.location));
			}
		}
		else
		{
			Breakpoint old = interpreter.clearBreakpoint(stmt.breakpoint.number);
			
			if (old != null)
			{
				println("Overwriting [" + old.number + "] " + old.location);
			}
			
			Breakpoint bp = interpreter.setBreakpoint(stmt, condition);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
	}

	/**
	 * Set a breakpoint at the given function or operation name with
	 * a condition.
	 *
	 * @param name The function or operation name.
	 * @param condition Any condition for the breakpoint, or null.
	 * @throws Exception Problems parsing condition.
	 */
	private void setBreakpoint(String name, String condition)
		throws Exception
	{
		LexTokenReader ltr = new LexTokenReader(name, Dialect.VDM_SL);
		LexToken token = ltr.nextToken();
		ltr.close();

		Value v = null;

		if (token.is(Token.IDENTIFIER))
		{
			LexIdentifierToken id = (LexIdentifierToken)token;
			TCNameToken lnt = new TCNameToken(id.location, interpreter.getDefaultName(), id.name);
			v = interpreter.findGlobal(lnt);
		}
		else if (token.is(Token.NAME))
		{
			v = interpreter.findGlobal(new TCNameToken((LexNameToken)token));
		}

		if (v instanceof FunctionValue)
		{
			FunctionValue fv = (FunctionValue)v;
			INExpression exp = fv.body;
			
			while (exp instanceof INBinaryExpression)
			{
				// None of the binary expressions check their BP, to avoid excessive stepping
				// when going through (say) a chain of "and" clauses. So if we've picked a
				// binary expression here, we move the BP to the left hand.
				INBinaryExpression bexp = (INBinaryExpression)exp;
				exp = bexp.left;
			}
			
			Breakpoint old = interpreter.clearBreakpoint(exp.breakpoint.number);
			
			if (old != null)
			{
				println("Overwriting [" + old.number + "] " + old.location);
			}
			
			Breakpoint bp = interpreter.setBreakpoint(exp, condition);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
		else if (v instanceof OperationValue)
		{
			OperationValue ov = (OperationValue)v;
			INStatement stmt = ov.body;
			Breakpoint old = interpreter.clearBreakpoint(stmt.breakpoint.number);
			
			if (old != null)
			{
				println("Overwriting [" + old.number + "] " + old.location);
			}
			
			Breakpoint bp = interpreter.setBreakpoint(stmt, condition);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
		else if (v == null)
		{
			println(name + " is not visible or not found");
		}
		else
		{
			println(name + " is not a function or operation");
		}
	}

	/**
	 * Set a tracepoint at the given file and line. Tracepoints without
	 * a condition just print "Reached [n]", where [n] is the breakpoint
	 * number.
	 *
	 * @param file The file name
	 * @param line The line number
	 * @param trace Any expression to evaluate at the tracepoint, or null
	 * @throws Exception Problems parsing condition.
	 */
	private void setTracepoint(File file, int line, String trace)
		throws Exception
	{
		if (file == null)
		{
			file = interpreter.getDefaultFile();
		}

		if (file == null || file.getPath().equals("?"))
		{
			Set<File> files = interpreter.getSourceFiles();
			
			if (files.size() > 1)
			{
				println("Assuming file " + file.getPath());
			}
			else if (files.isEmpty())
			{
				println("No files defined");
				return;
			}

			file = files.iterator().next();
		}

		INStatement stmt = interpreter.findStatement(file, line);

		if (stmt == null)
		{
			INExpression exp = interpreter.findExpression(file, line);

			if (exp == null)
			{
				println("No breakable expressions or statements at " + file + ":" + line);
			}
			else
			{
				Breakpoint old = interpreter.clearBreakpoint(exp.breakpoint.number);
				
				if (old != null)
				{
					println("Overwriting [" + old.number + "] " + old.location);
				}
				
				Breakpoint bp = interpreter.setTracepoint(exp, trace);
				println("Created " + bp);
				println(interpreter.getSourceLine(bp.location));
			}
		}
		else
		{
			Breakpoint old = interpreter.clearBreakpoint(stmt.breakpoint.number);
			
			if (old != null)
			{
				println("Overwriting [" + old.number + "] " + old.location);
			}

			Breakpoint bp = interpreter.setTracepoint(stmt, trace);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
	}

	/**
	 * Set a tracepoint at the given function or operation name. Tracepoints
	 * without a condition just print "Reached [n]", where [n] is the
	 * breakpoint number.
	 *
	 * @param name The function or operation name.
	 * @param trace Any trace for the tracepoint
	 * @throws Exception Problems parsing condition.
	 */
	private void setTracepoint(String name, String trace) throws Exception
	{
		LexTokenReader ltr = new LexTokenReader(name, Dialect.VDM_SL);
		LexToken token = ltr.nextToken();
		ltr.close();

		Value v = null;

		if (token.is(Token.IDENTIFIER))
		{
			LexIdentifierToken id = (LexIdentifierToken)token;
			TCNameToken lnt = new TCNameToken(id.location, interpreter.getDefaultName(), id.name);
			v = interpreter.findGlobal(lnt);
		}
		else if (token.is(Token.NAME))
		{
			v = interpreter.findGlobal(new TCNameToken((LexNameToken)token));
		}

		if (v instanceof FunctionValue)
		{
			FunctionValue fv = (FunctionValue)v;
			INExpression exp = fv.body;
			interpreter.clearBreakpoint(exp.breakpoint.number);
			Breakpoint bp = interpreter.setTracepoint(exp, trace);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
		else if (v instanceof OperationValue)
		{
			OperationValue ov = (OperationValue)v;
			INStatement stmt = ov.body;
			interpreter.clearBreakpoint(stmt.breakpoint.number);
			Breakpoint bp = interpreter.setTracepoint(stmt, trace);
			println("Created " + bp);
			println(interpreter.getSourceLine(bp.location));
		}
		else
		{
			println(name + " is not a function or operation");
		}
	}
}
