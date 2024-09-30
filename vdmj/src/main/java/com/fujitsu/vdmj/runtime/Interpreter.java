/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.runtime;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.modules.INModule;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.statements.INStatementList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.ConsoleWriter;
import com.fujitsu.vdmj.messages.VDMErrorsException;
import com.fujitsu.vdmj.scheduler.ResourceScheduler;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.traces.CallSequence;
import com.fujitsu.vdmj.traces.TraceFilter;
import com.fujitsu.vdmj.traces.TraceIterator;
import com.fujitsu.vdmj.traces.TraceReductionType;
import com.fujitsu.vdmj.traces.Verdict;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.values.Value;

/**
 * An abstract VDM interpreter.
 */
abstract public class Interpreter
{
	/** The main thread scheduler */
	protected ResourceScheduler scheduler;

	/** The initial execution context. */
	protected RootContext initialContext;

	/** A list of breakpoints created. */
	protected Map<Integer, Breakpoint> breakpoints;

	/** A list of source files loaded. */
	protected Map<File, SourceFile> sourceFiles;
	
	/** The number of the next breakpoint to be created. */
	protected int nextbreakpoint = 0;

	/** A static instance pointer to the interpreter. */
	protected static Interpreter instance = null;

	/** The saved initial context for trace execution */
	protected ByteArrayOutputStream savedInitialContext;

	/**
	 * Create an Interpreter.
	 */
	protected Interpreter()
	{
		this.scheduler = new ResourceScheduler();
		this.breakpoints = new TreeMap<Integer, Breakpoint>();
		this.sourceFiles = new HashMap<File, SourceFile>();
		
		instance = this;
	}
	
	/**
	 * Get the resource scheduler.
	 */
	public ResourceScheduler getScheduler()
	{
		return scheduler;
	}

	/**
	 * Get the initial root context.
	 */
	public RootContext getInitialContext()
	{
		return initialContext;
	}

	/**
	 * Get the global environment. In VDM-SL this is for the default module, and
	 * in VDM++ it is the global class environment.
	 */
	abstract public Environment getGlobalEnvironment();

	/**
	 * @return The Interpreter instance.
	 */
	public static Interpreter getInstance()
	{
		return instance;	// NB. last one created
	}

	/**
	 * Get the name of the default module or class. Symbols in the default
	 * module or class do not have to have their names qualified when being
	 * referred to on the command line.
	 *
	 * @return The default name.
	 */
	abstract public String getDefaultName();

	/**
	 * Get the filename that contains the default module or class.
	 *
	 * @return The default file name.
	 */
	abstract public File getDefaultFile();

	/**
	 * Set the default module or class name.
	 *
	 * @param name The default name.
	 * @throws Exception
	 */
	abstract public void setDefaultName(String name) throws Exception;

	/**
	 * Initialize the initial context. This means that all definition
	 * initializers are re-run to put the global environment back into its
	 * original state. This is run implicitly when the interpreter starts,
	 * but it can also be invoked explicitly via the "init" command.
	 *
	 * @throws Exception
	 */
	abstract public void init();

	/**
	 * Initialize the context between trace sequences. This is less
	 * thorough than the full init, since it does not reset the scheduler
	 * for example.
	 */
	abstract public void traceInit() throws Exception;

	/**
	 * Parse the line passed, type check it and evaluate it as an expression
	 * in the initial context.
	 *
	 * @param line A VDM expression.
	 * @return The value of the expression.
	 * @throws Exception Parser, type checking or runtime errors.
	 */
	abstract public Value execute(String line) throws Exception;

	/**
	 * Parse the line passed, and evaluate it as an expression in the context
	 * passed. This is used from debugger breakpoints.
	 *
	 * @param line A VDM expression.
	 * @param ctxt The context in which to evaluate the expression.
	 * @return The value of the expression.
	 * @throws Exception Parser or runtime errors.
	 */
	abstract public Value evaluate(String line, Context ctxt) throws Exception;

	/**
	 * Parse the content of the file passed, type check it and evaluate it as an
	 * expression in the initial context.
	 *
	 * @param file A file containing a VDM expression.
	 * @return The value of the expression.
	 * @throws Exception Parser, type checking or runtime errors.
	 */
	public Value execute(File file) throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(file));
		StringBuilder sb = new StringBuilder();

		String line = br.readLine();

		while (line != null)
		{
			sb.append(line);
			line = br.readLine();
		}

		br.close();

		Value result = execute(sb.toString());

		SchedulableThread.terminateAll();	// NB not a session (used for tests)
		return result;
	}

	/**
	 * @return The list of breakpoints currently set.
	 */
	public Map<Integer, Breakpoint> getBreakpoints()
	{
		return breakpoints;
	}
	
	/**
	 * @return The list of Catchpoints currently set.
	 */
	public List<Catchpoint> getCatchpoints()
	{
		List<Catchpoint> catchers = new Vector<Catchpoint>();
		
		for (Breakpoint bp: breakpoints.values())
		{
			if (bp instanceof Catchpoint)
			{
				catchers.add((Catchpoint) bp);
			}
		}
		
		return catchers;
	} 

	/**
	 * Get a line of a source file.
	 */
	public String getSourceLine(LexLocation src)
	{
		return getSourceLine(src.file, src.startLine);
	}

	/**
	 * Get a line of a source file by its location.
	 */
	public String getSourceLine(File file, int line)
	{
		return getSourceLine(file, line, ":  ");
	}

	/**
	 * Get a line of a source file by its location.
	 */
	public String getSourceLine(File file, int line, String sep)
	{
		try
		{
			SourceFile source = getSourceFile(file);
			return line + sep + source.getLine(line);
		}
		catch (IOException e)
		{
			return "Cannot open source file: " + file;
		}
	}

	/**
	 * Get an entire source file object.
	 * @throws IOException
	 */
	public SourceFile getSourceFile(File file) throws IOException
	{
		SourceFile source = sourceFiles.get(file);

		if (source == null)
		{
			source = new SourceFile(file);
			sourceFiles.put(file, source);
		}

		return source;
	}

	/**
	 * Get a list of all source files.
	 */
	abstract public Set<File> getSourceFiles();

	/**
	 * Find a statement by file name and line number.
	 *
	 * @param file The name of the class/module
	 * @param lineno The line number
	 * @return A INStatement object if found, else null.
	 */
	abstract public INStatement findStatement(File file, int lineno);
	abstract public INStatementList findStatements(File file, int lineno);

	/**
	 * Find an expression by file name and line number.
	 *
	 * @param file The name of the file
	 * @param lineno The line number
	 * @return An INExpression object if found, else null.
	 */
	abstract public INExpression findExpression(File file, int lineno);
	abstract public INExpressionList findExpressions(File file, int lineno);

	/**
	 * Find a global environment value by name.
	 *
	 * @param name The name of the variable
	 * @return A Value object if found, else null.
	 */
	public Value findGlobal(TCNameToken name)
	{
		return initialContext.check(name);
	}

	/**
	 * Set a statement tracepoint. A tracepoint does not stop execution, but
	 * evaluates and displays an expression before continuing.
	 *
	 * @param stmt The statement to trace.
	 * @param trace The expression to evaluate.
	 * @return The Breakpoint object created.
	 *
	 * @throws Exception INExpression is not valid.
	 */
	public Breakpoint setTracepoint(INStatement stmt, String trace) throws Exception
	{
		stmt.breakpoint = new Tracepoint(stmt.location, ++nextbreakpoint, trace);
		breakpoints.put(nextbreakpoint, stmt.breakpoint);
		return stmt.breakpoint;
	}

	/**
	 * Set an expression tracepoint. A tracepoint does not stop execution, but
	 * evaluates an expression before continuing.
	 *
	 * @param exp The expression to trace.
	 * @param trace The expression to evaluate.
	 * @return The Breakpoint object created.
	 * @throws Exception 
	 */
	public Breakpoint setTracepoint(INExpression exp, String trace) throws Exception
	{
		exp.breakpoint = new Tracepoint(exp.location, ++nextbreakpoint, trace);
		breakpoints.put(nextbreakpoint, exp.breakpoint);
		return exp.breakpoint;
	}

	/**
	 * Set a statement breakpoint. A breakpoint stops execution and allows
	 * the user to query the environment.
	 *
	 * @param stmt The statement at which to stop.
	 * @param condition The condition when to stop.
	 * @return The Breakpoint object created.
	 * @throws Exception 
	 */
	public Breakpoint setBreakpoint(INStatement stmt, String condition) throws Exception
	{
		stmt.breakpoint = new Stoppoint(stmt.location, ++nextbreakpoint, condition);
		breakpoints.put(nextbreakpoint, stmt.breakpoint);
		return stmt.breakpoint;
	}

	/**
	 * Set an expression breakpoint. A breakpoint stops execution and allows
	 * the user to query the environment.
	 *
	 * @param exp The expression at which to stop.
	 * @param condition The condition when to stop.
	 * @return The Breakpoint object created.
	 * @throws Exception 
	 *
	 */
	public Breakpoint setBreakpoint(INExpression exp, String condition) throws Exception
	{
		exp.breakpoint = new Stoppoint(exp.location, ++nextbreakpoint, condition);
		breakpoints.put(nextbreakpoint, exp.breakpoint);
		return exp.breakpoint;
	}

	/**
	 * Set an exception catchpoint. This stops execution at the point that a matching
	 * exception is thrown.
	 *
	 * @param exp The exception value(s) at which to stop, or null for any exception.
	 * @return The Breakpoint object created.
	 * @throws Exception 
	 */
	public List<Breakpoint> setCatchpoint(String value) throws Exception
	{
		List<Breakpoint> values = new Vector<Breakpoint>();
		
		/**
		 * Parse each expression, so the string value is "canonical".
		 */
		if (value != null)
		{
			LexTokenReader ltr = new LexTokenReader(value, Dialect.VDM_SL);
			ltr.nextToken();
			ExpressionReader er = new ExpressionReader(ltr);
			
			while (ltr.getLast().isNot(Token.EOF))
			{
				ASTExpression exp = er.readExpression();
				Catchpoint catcher = new Catchpoint(exp.toString(), ++nextbreakpoint);
				breakpoints.put(nextbreakpoint, catcher);
				values.add(catcher);
			}
		}
		else
		{
			Catchpoint catcher = new Catchpoint(null, ++nextbreakpoint);
			breakpoints.put(nextbreakpoint, catcher);
			values.add(catcher);
		}
		
		return values;
	}

	/**
	 * Clear the breakpoint given by the number.
	 *
	 * @param bpno The breakpoint number to remove.
	 * @return The breakpoint object removed, or null.
	 */
	public Breakpoint clearBreakpoint(int bpno)
	{
		Breakpoint old = breakpoints.remove(bpno);

		if (old != null && !(old instanceof Catchpoint))
		{
			INStatement stmt = findStatement(old.location.file, old.location.startLine);

			if (stmt != null)
			{
				stmt.breakpoint = new Breakpoint(stmt.location);
			}
			else
			{
				INExpression exp = findExpression(old.location.file, old.location.startLine);
				assert (exp != null) : "Cannot locate old breakpoint?";
				exp.breakpoint = new Breakpoint(exp.location);
			}
		}

		return old;		// null if not found
	}

	public void clearBreakpointHits()
	{
		for (Entry<Integer, Breakpoint> e: breakpoints.entrySet())
		{
			e.getValue().clearHits();
		}
	}

	/**
	 * Parse an expression line into a TC tree, ready to be type checked. 
	 */
	abstract protected TCExpression parseExpression(String line, String module) throws Exception;

	/**
	 * Type check a TC expression tree passed.
	 */
	public TCType typeCheck(TCNode tree) throws Exception
	{
		TypeChecker.clearErrors();
		TCType type = null;
		
		if (tree instanceof TCExpression)
		{
			TCExpression exp = (TCExpression)tree;
			type = exp.typeCheck(getGlobalEnvironment(), null, NameScope.NAMESANDSTATE, null);
		}
		else if (tree instanceof TCStatement)
		{
			TCStatement stmt = (TCStatement)tree;
			type = stmt.typeCheck(getGlobalEnvironment(), NameScope.NAMESANDSTATE, null, false);
		}
		else
		{
			throw new Exception("Cannot type check " + tree.getClass().getSimpleName());
		}

		if (TypeChecker.getErrorCount() > 0)
		{
			throw new VDMErrorsException(TypeChecker.getErrors());
		}

		return type;
	}

	/**
	 * @param classname 
	 */
	public INClassDefinition findClass(String classname)
	{
		assert false : "findClass cannot be called for executableModules";
		return null;
	}

	/**
	 * @param module  
	 */
	public INModule findModule(String module)
	{
		assert false : "findModule cannot be called for classes";
		return null;
	}

	private static ConsoleWriter writer = null;

	public static void setTraceOutput(ConsoleWriter pw)
	{
		writer = pw;
	}

	abstract public INNamedTraceDefinition findTraceDefinition(TCNameToken name);

	abstract public Context getTraceContext(INClassDefinition classdef) throws ValueException;

	public void runtrace(String name, int startTest, int endTest, boolean debug)
		throws Exception
	{
		runtrace(name, startTest, endTest, debug, 1.0F, TraceReductionType.NONE, 1234);
	}

	public boolean runtrace(
		String name, int startTest, int endTest, boolean debug,
		float subset, TraceReductionType reductionType, long seed)
		throws Exception
	{
		// Trace names have / substituted for _ to make a valid name during the parse
		name = name.replaceAll("/", "_");

		LexTokenReader ltr = new LexTokenReader(name, Dialect.VDM_SL);
		LexToken token = ltr.nextToken();
		ltr.close();
		TCNameToken lexname = null;

		switch (token.type)
		{
			case NAME:
				lexname = new TCNameToken((LexNameToken) token);

				if (Settings.dialect == Dialect.VDM_SL &&
					!lexname.getModule().equals(getDefaultName()))
				{
					setDefaultName(lexname.getModule());
				}
				break;

			case IDENTIFIER:
				lexname = new TCNameToken(token.location, getDefaultName(), ((LexIdentifierToken)token).name);
				break;

			default:
				throw new Exception("Expecting trace name");
		}

		INNamedTraceDefinition tracedef = findTraceDefinition(lexname);

		if (tracedef == null)
		{
			throw new Exception("Trace " + lexname + " not found");
		}

		long before = System.currentTimeMillis();
		TraceIterator tests = tracedef.getIterator(getTraceContext(tracedef.classDefinition));
		long after = System.currentTimeMillis();

		if (writer == null)
		{
			writer = Console.out;
		}

		final int count = tests.count();

		if (endTest > count)
		{
			throw new Exception("Trace " + lexname + " only has " + count + " tests");
		}
		
		if (endTest == 0)		// To the end of the tests, if specified as zero
		{
			endTest = count;
		}
		
		if (startTest > 0)		// Suppress any reduction if a range specified
		{
			subset = 1.0F;
			reductionType = TraceReductionType.NONE;
		}

		int testNumber = 1;
		int excluded = 0;
		boolean failed = false;
		TraceFilter filter = new TraceFilter(count, subset, reductionType, seed);

		if (filter.getFilteredCount() > 0)	// Only known for random reduction
		{
			writer.print("Generated " + count + " tests, reduced to " + filter.getFilteredCount() + ",");
		}
		else
		{
			writer.print("Generated " + count + " tests");
			
			if (subset < 1.0)
			{
				writer.print(", reduced by " + reductionType + ",");
			}
		}
		
		writer.println(" in " + (double)(after-before)/1000 + " secs. ");
		before = System.currentTimeMillis();
		
		// Not needed with new traces?
		// Environment environment = getTraceEnvironment(tracedef.classDefinition);

		while (tests.hasMoreTests())
		{
			CallSequence test = tests.getNextTest();
			
			if (testNumber < startTest || testNumber > endTest || filter.isRemoved(test, testNumber))
			{
				excluded++;
			}
			else if (filter.getFilteredBy(test) > 0)
			{
				excluded++;
    			writer.println("Test " + testNumber + " = " + test.getCallString(getTraceContext(tracedef.classDefinition)));
				writer.println("Test " + testNumber + " FILTERED by test " + filter.getFilteredBy(test));
			}
			else
			{
				// test.typeCheck(this, environment);	// Not needed with new traces?
				
    			traceInit();	// Initialize completely between every run...
    			List<Object> result = runOneTrace(tracedef.classDefinition, test, debug);
    			filter.update(result, test, testNumber);

    			writer.println("Test " + testNumber + " = " + test.getCallString(getTraceContext(tracedef.classDefinition)));
    			writer.println("Result = " + result);
    			
    			if (result.lastIndexOf(Verdict.PASSED) == -1)
    			{
    				failed = true;	// Not passed => failed.
    			}
			}

			if (testNumber >= endTest)
			{
				excluded = count - (endTest - startTest + 1);
				break;
			}

			testNumber++;
		}

		init();
		savedInitialContext = null;
		
		if (excluded > 0)
		{
			writer.println("Excluded " + excluded + " tests");
		}

		long finished = System.currentTimeMillis();
		writer.println("Executed in " + (double)(finished-after)/1000 + " secs. ");
		
		return !failed;
	}

	public abstract List<Object> runOneTrace(INClassDefinition classDefinition, CallSequence test, boolean debug);
	
	/**
	 * Return the executable AST that is loaded in the interpreter, rather than the
	 * one that is loaded in the INPlugin (ie. differences mean that the spec has changed).
	 */
	public abstract <T> T getIN();
}
