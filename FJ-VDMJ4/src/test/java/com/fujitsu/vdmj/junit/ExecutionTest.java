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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.junit;

import java.io.File;
import java.net.URL;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.commands.CommandReader;
import com.fujitsu.vdmj.commands.ModuleCommandReader;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import junit.framework.TestCase;

public class ExecutionTest extends TestCase
{
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		Settings.release = Release.CLASSIC;
		Settings.dialect = Dialect.VDM_SL;
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	private void process(String resource, String aresource) throws Exception
	{
		Console.out.println("Processing " + resource + "...");

		URL rurl = getClass().getResource("/exectest/" + resource);
		String file = rurl.getPath();
		URL aurl = getClass().getResource("/exectest/" + aresource);
		String assertions = aurl.getPath();

		long before = System.currentTimeMillis();
		LexTokenReader ltr = new LexTokenReader(new File(file), Dialect.VDM_SL);
		ModuleReader mr = new ModuleReader(ltr);
		ASTModuleList parsed = new ASTModuleList();
		parsed.addAll(mr.readModules());
		mr.close();

		long after = System.currentTimeMillis();
		Console.out.println("Parsed " + parsed.size() + " modules in " +
   			(double)(after-before)/1000 + " secs. ");
		mr.printErrors(Console.out);
		mr.printWarnings(Console.out);
		assertEquals("Parse errors", 0, mr.getErrorCount());

		before = System.currentTimeMillis();
		TCModuleList checked = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsed);
		TypeChecker typeChecker = new ModuleTypeChecker(checked);
		typeChecker.typeCheck();
		after = System.currentTimeMillis();
   		Console.out.println("Type checked in " + (double)(after-before)/1000 + " secs. ");
		Console.out.println("There were " + TypeChecker.getWarningCount() + " warnings");
		TypeChecker.printErrors(Console.out);
		assertEquals("Type check errors", 0, TypeChecker.getErrorCount());

		INModuleList runnable = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checked);
		ModuleInterpreter interpreter = new ModuleInterpreter(runnable, checked);
		interpreter.init();
		CommandReader reader = new ModuleCommandReader(interpreter, "");
		boolean OK = reader.assertFile(new File(assertions));
		assertEquals("Execution errors", true, OK);
	}

	private void interpret(String resource) throws Exception
	{
		URL url = getClass().getResource("/exectest/" + resource);
		String file = url.getPath();

		ModuleInterpreter interpreter = new ModuleInterpreter(new INModuleList(), new TCModuleList());
		interpreter.init();
		CommandReader reader = new ModuleCommandReader(interpreter, "");
		boolean OK = reader.assertFile(new File(file));
		assertEquals("Execution errors", true, OK);
	}

	public void testExpressions() throws Exception
	{
		interpret("basic.tests");
	}

	public void testMADJ21() throws Exception
	{
		process("MADJ-21.vdm", "MADJ-21.tests");
	}

	public void testFunction() throws Exception
	{
		process("function.vdm", "function.tests");
	}

	public void testOperation() throws Exception
	{
		process("operation.vdm", "operation.tests");
	}

	public void testSigma() throws Exception
	{
		process("sigma.vdm", "sigma.tests");
	}

	public void testFlat() throws Exception
	{
		process("flat.vdm", "flat.tests");
	}
}
