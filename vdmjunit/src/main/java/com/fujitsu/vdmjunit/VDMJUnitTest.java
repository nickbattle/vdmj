/*******************************************************************************
 *
 *	Copyright (c) 2013 Fujitsu Services Ltd.
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

package com.fujitsu.vdmjunit;

import static org.junit.Assert.fail;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.traces.TraceReductionType;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.Value;

/**
 * The parent class for all VDMJUnit tests. This class provides test methods
 * that are common to all dialects.
 */
public abstract class VDMJUnitTest
{
	/** The VDM interpreter. */
	protected static Interpreter interpreter;
	
	/** True if the init() method has been called. */
	private static boolean initialized = false;
	
	/** The reader used to parse the spec */
	protected static SpecificationReader reader;
	
	/**
	 * Set the release of VDM before parsing. The release is either CLASSIC
	 * or VDM10. The default is CLASSIC.
	 * 
	 * @param release The release to use.
	 */
	protected static void setRelease(Release release)
	{
		Settings.release = release;
	}

	/**
	 * Parse and type check source files, ready for testing. The files are
	 * located relative to the Java classpath, so you may need to extend the
	 * classpath to include the root of your files. If a file passed is a
	 * directory, then all of the VDM source files found within the directory
	 * will be loaded (but not subdirectories). The list of files can be empty,
	 * which will create an empty specification. If the specification has syntax
	 * or type checking errors, then a JUnit fail will be executed and the test
	 * suite will stop.
	 * 
	 * @param files A list of VDM source files or directories.
	 * @throws Exception 
	 */
	protected static void readSpecification(String... files) throws Exception
	{
		fail("Implemented in subclasses only");
	}

	/**
	 * As readSpecification, but with a specified charset.
	 * 
	 * @param charset The character set to load the specifications.
	 * @param files The files to load.
	 * @throws Exception
	 */
	protected static void readSpecification(Charset charset, String... files) throws Exception
	{
		fail("Implemented in subclasses only");
	}
	
	/**
	 * Return a list of syntax or type checking errors raised by readSpecification.
	 */
	protected static List<VDMError> getErrors()
	{
		if (reader != null)
		{
			return reader.getErrors();
		}
		else
		{
			return new Vector<VDMError>();
		}
	}

	/**
	 * Return a list of warnings raised by readSpecification.
	 */
	protected static List<VDMWarning> getWarnings()
	{
		if (reader != null)
		{
			return reader.getWarnings();
		}
		else
		{
			return new Vector<VDMWarning>();
		}
	}

	/**
	 * Initialize, or re-initialize the VDM specification. This is often called
	 * in an @Before method, to reset the specification to the same state before
	 * every test. Note that this method must be called before any of the run
	 * methods.
	 */
	protected static void init()
	{
		interpreter.init();
		initialized = true;
	}
	
	/**
	 * Change the default module or class to the name passed. In VDM-SL, the default
	 * module defines the functions and operations that can be seen (the module's
	 * own, plus those that are imported). In all dialects, names from the default
	 * module can be used without qualification, eg. "op(1)" rather than "A`op(1)". 
	 * 
	 * @param name The default module or class name.
	 * @throws Exception If the name cannot be found in the specification.
	 */
	protected static void setDefault(String name) throws Exception
	{
		interpreter.setDefaultName(name);
	}

	/**
	 * Create a named temporary value. Such values can then be used in subsequent
	 * run calls, until the next init().
	 * 
	 * @param name The variable name.
	 * @param value The expression to evaluate for the name.
	 * @throws Exception Syntax, type checking or runtime errors in the evaluation.
	 */
	protected void create(String name, String value) throws Exception
	{
		fail("Create only available for VDM++ and VDM-RT");
	}
	
	/**
	 * Run all the tests in a combinatorial trace. The test results are sent to stdout
	 * as the test suite executes. If all tests produce a PASSED verdict, then the
	 * method returns true, else if there are any FAILED or INDETERMINATE, the method
	 * returns false. Test results are send to stdout.
	 * 
	 * @param name The name of the trace to run.
	 * @return True, if all generated tests pass.
	 * @throws Exception
	 */
	protected boolean runTrace(String name) throws Exception
	{
		return interpreter.runtrace(name, 0, 0, false, 0, TraceReductionType.NONE, 0);
	}

	/**
	 * Run a range of tests from a combinatorial trace. Tests are numbered from 1. The test results
	 * are sent to stdout as the test suite executes. If all tests produce a PASSED verdict,
	 * then the method returns true, else if there are any FAILED or INDETERMINATE, the method
	 * returns false. Test results are send to stdout.
	 * 
	 * @param name The name of the trace to run.
	 * @param startTest The first test number to run.
	 * @param endTest The last test number to run - zero means to the end.
	 * @return True, if the test passes.
	 * @throws Exception
	 */
	protected boolean runTrace(String name, int startTest, int endTest) throws Exception
	{
		return interpreter.runtrace(name, startTest, endTest, false, 0, TraceReductionType.NONE, 0);
	}

	/**
	 * Run a reduced set of tests from a combinatorial trace. The subset argument defines a
	 * number from 0-1, indicating the proportion of the generated tests that should be
	 * selected for execution. The reduction type argument determines the method of selection
	 * of the tests. To allow the same subset to be selected repeatably, the seed argument
	 * re-seeds the random number generator used to select the tests. If all selected tests
	 * produce a PASSED verdict, then the method returns true, else if there are any FAILED
	 * or INDETERMINATE, the method returns false. Test results are send to stdout.
	 * 
	 * @param name The name of the trace to run.
	 * @param subset The proportion of tests to select, from 0-1.
	 * @param type The trace reduction strategy.
	 * @param seed A seed for repeatable reduction selection.
	 * @return True, if the test passes.
	 * @throws Exception
	 */
	protected boolean runTrace(String name, double subset, TraceReductionType type, int seed)
		throws Exception
	{
		return interpreter.runtrace(name, 0, 0, false, (float)subset, type, seed);
	}

	/**
	 * Parse and evaluate the expression passed in the current specification. The init
	 * method must have been called beforehand. The result is a VDMJ Value object,
	 * which can represent any VDM value (set, map, seq etc).
	 * 
	 * @param expression The expression to evaluate.
	 * @return A VDMJ Value.
	 * @throws Exception Lexical, syntax or type checking errors found in expression,
	 * 			or the specification has not been initialized.
	 */
	protected Value run(String expression) throws Exception
	{
		if (!initialized)
		{
			throw new RuntimeException("Specification not initialized - call init()");
		}
		
		return interpreter.execute(expression);
	}

	/**
	 * Same as run, but convert the result to a Java long. This is the same as
	 * calling the intValue method on the Value returned from run.
	 * 
	 * @param expression The expression to evaluate.
	 * @return A long.
	 * @throws Exception Lexical, syntax or type checking errors found in expression,
	 * 			or the specification has not been initialized, or if the result is not
	 * 			a VDM integer value.
	 */
	protected long runInt(String expression) throws Exception
	{
		return run(expression).intValue(null);
	}

	/**
	 * Same as run, but convert the result to a Java double. This is the same as
	 * calling the realValue method on the Value returned from run.
	 * 
	 * @param expression The expression to evaluate.
	 * @return A double.
	 * @throws Exception Lexical, syntax or type checking errors found in expression,
	 * 			or the specification has not been initialized, or if the result is not
	 * 			a VDM real value.
	 */
	protected double runReal(String expression) throws Exception
	{
		return run(expression).realValue(null);
	}

	/**
	 * Same as run, but convert the result to a Java boolean. This is the same as
	 * calling the boolValue method on the Value returned from run.
	 * 
	 * @param expression The expression to evaluate.
	 * @return A boolean.
	 * @throws Exception Lexical, syntax or type checking errors found in expression,
	 * 			or the specification has not been initialized, or if the result is not
	 * 			a VDM bool value.
	 */
	protected boolean runBool(String expression) throws Exception
	{
		return run(expression).boolValue(null);
	}
	
	/**
	 * Assert that the result Value passed, when substituted into the VDM assertion passed
	 * as RESULT, evaluates to true.
	 * 
	 * @param result The RESULT being tested.
	 * @param assertion A VDM assertion about RESULT.
	 * @throws Exception Thrown if the assertion expression is not boolean.
	 */
	protected void assertVDM(String message, Value result, String assertion) throws Exception
	{
		Context ctxt = new Context(LexLocation.ANY, "Assertion context", interpreter.getInitialContext());
		TCNameToken rname = new TCNameToken(LexLocation.ANY, interpreter.getDefaultName(), "RESULT");
		ctxt.put(rname, result);
		Value res = interpreter.evaluate(assertion, ctxt);
		
		if (!(res instanceof BooleanValue))
		{
			throw new Exception("Expression is not boolean. Value = " + res);
		}
		else
		{
			BooleanValue bv = (BooleanValue)res;
			
			if (!bv.value)
			{
				throw new AssertionError(message == null ? "Assertion failed" : message);
			}
		}
	}
	
	/**
	 * Assert that the result Value passed, when substituted into the VDM assertion passed
	 * as RESULT, evaluates to true. Uses the default assertion failure message.
	 */
	protected void assertVDM(Value result, String assertion) throws Exception
	{
		assertVDM(null, result, assertion);
	}

	/**
	 * Assert that the VDM expression passed, when evaluated and substituted as RESULT 
	 * into the VDM assertion passed, evaluates to true.
	 * 
	 * @param expression The expression to evaluate and test.
	 * @param assertion The VDM assertion about the RESULT of the expression.
	 * @throws Exception Thrown if the assertion expression is not boolean.
	 */
	protected void assertVDM(String message, String expression, String assertion) throws Exception
	{
		assertVDM(message, run(expression), assertion);
	}
	
	/**
	 * Assert that the VDM expression passed, when evaluated and substituted as RESULT 
	 * into the VDM assertion passed, evaluates to true. Uses the default assertion
	 * failure message.
	 */ 
	protected void assertVDM(String expression, String assertion) throws Exception
	{
		assertVDM(null, run(expression), assertion);
	}
}
