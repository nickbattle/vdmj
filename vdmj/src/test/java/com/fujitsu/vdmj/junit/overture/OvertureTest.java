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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.junit.overture;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTBUSClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTCPUClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.ConsolePrintWriter;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.UndefinedValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.VoidValue;

import junit.framework.TestCase;

abstract public class OvertureTest extends TestCase
{
	private String vppName = null;
	private String assertName = null;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		Settings.dialect = Dialect.VDM_PP;
		Settings.release = Release.DEFAULT;
		Settings.strict = true;
		Properties.init();
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	protected void syntax(String rpath)
	{
		setNames("/Overture/syntax/", rpath);
		List<VDMMessage> actual = new Vector<VDMMessage>();
		parse(actual);
		checkErrors(actual);
	}

	protected void typecheck(String rpath)
	{
		setNames("/Overture/typecheck/", rpath);
		List<VDMMessage> actual = new Vector<VDMMessage>();
		ASTClassList parsed = parse(actual);

		if (!actual.isEmpty())
		{
			Console.out.println(Utils.listToString(actual, "\n"));
			assertEquals("Expecting no syntax errors", 0, actual.size());
		}

		try
		{
			TCClassList checked = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsed);
			parsed = null;	// Not needed now
			TypeChecker typeChecker = new ClassTypeChecker(checked);
			typeChecker.typeCheck();
			TypeChecker.printErrors(Console.out);
			TypeChecker.printWarnings(Console.out);

			actual.addAll(TypeChecker.getErrors());
			actual.addAll(TypeChecker.getWarnings());
			checkErrors(actual);
		}
		catch (Exception e)
		{
			fail("Caught: " + e + " in " + assertName);
		}
	}

	protected void runtime(String rpath)
	{
		setNames("/Overture/runtime/", rpath);
		List<VDMMessage> actual = new Vector<VDMMessage>();
		ASTClassList parsed = parse(actual);

		if (!actual.isEmpty())
		{
			Console.out.println(Utils.listToString(actual, "\n"));
			assertEquals("Expecting no syntax errors", 0, actual.size());
		}

		if (Settings.dialect == Dialect.VDM_RT)
		{
			try
			{
				parsed.add(new ASTCPUClassDefinition());
				parsed.add(new ASTBUSClassDefinition());
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);	// Should never happen
			}
		}

		try
		{
			TCClassList checked = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsed);
			parsed = null;	// Not needed now
			TypeChecker typeChecker = new ClassTypeChecker(checked);
			typeChecker.typeCheck();
			TypeChecker.printErrors(Console.out);
			TypeChecker.printWarnings(Console.out);

			actual.addAll(TypeChecker.getErrors());
			actual.addAll(TypeChecker.getWarnings());

			if (!actual.isEmpty())
			{
				Console.out.println(Utils.listToString(actual, "\n"));
				assertEquals("Expecting no typecheck errors", 0, actual.size());
			}

			INClassList runnable = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checked);
			Interpreter interpreter = new ClassInterpreter(runnable, checked);
			interpreter.init();

			interpreter.execute(new File(assertName));
			fail("Expecting a runtime error");
		}
		catch (ContextException e)
		{
			Console.out.println(e.toString());
			actual.add(new VDMError(e));
			checkErrors(actual);
		}
		catch (Exception e)
		{
			fail("Caught: " + e + " in " + assertName);
		}
	}

	protected static enum ResultType
	{
		TRUE, VOID, UNDEFINED, ERROR
	}

	protected void evaluate(String rpath, ResultType rt)
	{
		evaluate(rpath, rt, 0);
	}

	protected void evaluate(String rpath, ResultType rt, int error)
	{
		evaluate(rpath, rt, error, Release.CLASSIC);
	}

	protected void evaluate(String rpath, ResultType rt, int error, Release release)
	{
		Settings.release = release;

		setNames("/Overture/evaluate/", rpath);
		List<VDMMessage> actual = new Vector<VDMMessage>();
		ASTClassList parsed = parse(actual);

		if (!actual.isEmpty())
		{
			Console.out.println(Utils.listToString(actual, "\n"));
			assertEquals("Expecting no syntax errors", 0, actual.size());
		}
		
		if (Settings.dialect == Dialect.VDM_RT)
		{
			try
			{
				parsed.add(new ASTCPUClassDefinition());
				parsed.add(new ASTBUSClassDefinition());
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);	// Should never happen
			}
		}

		try
		{
    		TCClassList checked = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsed);
    		parsed = null;	// Not needed now
    		TypeChecker typeChecker = new ClassTypeChecker(checked);
    		typeChecker.typeCheck();
    		TypeChecker.printErrors(Console.out);
    		TypeChecker.printWarnings(Console.out);
    
    		actual.addAll(TypeChecker.getErrors());
    		actual.addAll(TypeChecker.getWarnings());
    
    		if (!actual.isEmpty())
    		{
    			Console.out.println(Utils.listToString(actual, "\n"));
    			assertEquals("Expecting no typecheck errors", 0, actual.size());
    		}
    
			INClassList runnable = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checked);
			Interpreter interpreter = new ClassInterpreter(runnable, checked);
			interpreter.init();

			Value result = interpreter.execute(new File(assertName));

			Console.out.println("Result = " + result);
			Value expected = null;

			switch (rt)
			{
				case TRUE:
					expected = new BooleanValue(true);
					break;

				case VOID:
					expected = new VoidValue();
					break;

				case UNDEFINED:
					expected = new UndefinedValue();
					break;
					
				default:
					break;
				}

			assertEquals("Evaluation error", expected, result);
			assertTrue("Expecting runtime error " + error, error == 0);
		}
		catch (ContextException e)
		{
			Console.out.println(e.toString());

			if (e.number != error)
			{
				fail("Unexpected runtime error: " + e);
			}
		}
		catch (Exception e)
		{
			fail("Caught: " + e + " in " + assertName);
		}
	}

	protected void combtest(String rpath, String testExp)
	{
		combtest(rpath, rpath, testExp, 0);	// No expected error
	}

	protected void combtest(String rpath, String testExp, int error)
	{
		combtest(rpath, rpath, testExp, error);
	}

	protected void combtest(String vpath, String apath, String testExp, int error)
	{
		Console.out.println("Processing " + apath + "...");

		setVppName("/Overture/combtest/", vpath);
		setAssertName("/Overture/combtest/", apath);

		List<VDMMessage> actual = new Vector<VDMMessage>();
		ASTClassList parsed = parse(actual);

		if (!actual.isEmpty())
		{
			Console.out.println(Utils.listToString(actual, "\n"));
			assertEquals("Expecting no syntax errors", 0, actual.size());
		}

		try
		{
			TCClassList checked = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsed);
			parsed = null;	// Not needed now
			TypeChecker typeChecker = new ClassTypeChecker(checked);
			typeChecker.typeCheck();
			TypeChecker.printErrors(Console.out);
			TypeChecker.printWarnings(Console.out);

			actual.addAll(TypeChecker.getErrors());
			// actual.addAll(TypeChecker.getWarnings());

			if (!actual.isEmpty())
			{
				Console.out.println(Utils.listToString(actual, "\n"));
				assertEquals("Expecting no typecheck errors", 0, actual.size());
			}

			INClassList runnable = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checked);
			Interpreter interpreter = new ClassInterpreter(runnable, checked);
			interpreter.init();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ConsolePrintWriter pw = new ConsolePrintWriter(out);
			// TraceStatement.setOutput(pw);
			Interpreter.setTraceOutput(pw);

			//interpreter.execute(testExp, null);
			interpreter.runtrace(testExp, 0, 0, false);

			pw.close();
			String result = out.toString();
			String expected = readFile(new File(assertName));

			if (!result.equals(expected))
			{
				Console.out.println(assertName + " actual:\n" + result);
				Console.out.println("\n" + assertName + " expected:\n" + expected);
			}

			assertEquals("Evaluation error", expected, result);
			assertTrue("Expecting runtime error " + error, error == 0);
		}
		catch (ContextException e)
		{
			Console.out.println(e.toString());

			if (e.number != error)
			{
				fail("Unexpected runtime error: " + e);
			}
		}
		catch (Exception e)
		{
			fail("Caught: " + e + " in " + assertName);
		}
	}

	private void checkErrors(List<VDMMessage> actual)
	{
		try
		{
			Interpreter interpreter = new ClassInterpreter(new INClassList(), new TCClassList());
			interpreter.init();

			Value assertions = interpreter.execute(new File(assertName));

			assertTrue("Expecting error list", assertions instanceof SeqValue);

			List<Long> expected = new Vector<Long>();

			for (Value ex: assertions.seqValue(null))
			{
				long n = ex.intValue(null);
				expected.add(n);
			}

			List<Long> actNums = new Vector<Long>();

			for (VDMMessage m: actual)
			{
				actNums.add((long)m.number);
			}

			if (!actNums.equals(expected))
			{
				Console.out.println("Expected errors: " + listErrNos(expected));
				Console.out.println("Actual errors: " + listErrs(actual));
				Console.out.println(Utils.listToString(actual, "\n"));
				fail("Actual errors not as expected");
			}
		}
		catch (Exception e)
		{
			fail("Caught: " + e + " in " + assertName);
		}
	}

	private String readFile(File file) throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(file));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(out);

		String line = br.readLine();

		while (line != null)
		{
			pw.println(line);
			line = br.readLine();
		}

		br.close();
		pw.close();
		return out.toString();	// Same EOL convention as local machine
	}

	private void setNames(String prefix, String root)
	{
		setVppName(prefix, root);
		setAssertName(prefix, root);
		Console.out.println("Processing " + prefix + root + "...");
	}

	private void setVppName(String prefix, String root)
	{
		URL rurl = getClass().getResource(prefix + root + ".vpp");

		if (rurl == null)
		{
			fail("Cannot find resource: " + prefix + root + ".vpp");
		}

		vppName = rurl.getPath();
	}

	private void setAssertName(String prefix, String root)
	{
		URL rurl = getClass().getResource(prefix + root + ".assert");

		if (rurl == null)
		{
			fail("Cannot find resource: " + prefix + root + ".assert");
		}

		assertName = rurl.getPath();
	}

	private ASTClassList parse(List<VDMMessage> messages)
	{
		ASTClassList classes = null;

		LexTokenReader ltr = new LexTokenReader(new File(vppName), Dialect.VDM_RT);
		ClassReader cr = new ClassReader(ltr);
		classes = cr.readClasses();
		cr.close();
		messages.addAll(cr.getErrors());
		messages.addAll(cr.getWarnings());

		return classes;
	}

	private String listErrs(List<VDMMessage> list)
	{
		StringBuilder sb = new StringBuilder("[");
		String sep = "";

		for (VDMMessage m: list)
		{
			sb.append(sep);
			sb.append(m.number);
			sep = ", ";
		}

		sb.append("]");
		return sb.toString();
	}

	private String listErrNos(List<Long> list)
	{
		StringBuilder sb = new StringBuilder("[");
		String sep = "";

		for (Long m: list)
		{
			sb.append(sep);
			sb.append(m);
			sep = ", ";
		}

		sb.append("]");
		return sb.toString();
	}
}
