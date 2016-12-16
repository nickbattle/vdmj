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
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import junit.framework.TestCase;

public class ModuleTest extends TestCase
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

	private void process(String resource, Integer... expected) throws Exception
	{
		Console.out.println("Processing " + resource + "...");

		URL rurl = getClass().getResource("/modtest/" + resource);
		String file = rurl.getPath();

		long before = System.currentTimeMillis();
		LexTokenReader ltr = new LexTokenReader(new File(file), Dialect.VDM_SL);
		ModuleReader mr = new ModuleReader(ltr);
		ASTModuleList parsed = new ASTModuleList();
		parsed.addAll(mr.readModules());
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

		//assertEquals("Type check errors", 0, TypeChecker.getErrorCount());
		assertEquals("Type check errors", expected.length, TypeChecker.getErrorCount());

		int p = 0;
		
		for (VDMError err: TypeChecker.getErrors())
		{
			assertEquals("Type checking error", err.number, expected[p++].intValue());
		}
	}

	public void testRailway() throws Exception
	{
		process("railway.vdm");
	}

	public void testCSKExample() throws Exception
	{
		process("cskexample.vdm");
	}

	public void testBar() throws Exception
	{
		process("bar.vdm");
	}

	public void testNDB() throws Exception
	{
		process("ndb.vdm");
	}

	public void testLoose() throws Exception
	{
		process("loose.vdm");
	}

	public void testSoccer() throws Exception
	{
		process("soccer.vdm");
	}

	public void testSimulator() throws Exception
	{
		process("simulator1.vdm");
	}

	public void testGraphEd() throws Exception
	{
		process("graph-ed.vdm");
	}

	public void testProg() throws Exception
	{
		process("proglang.vdm");
	}

	public void testMetro() throws Exception
	{
		process("metro.vdm");
	}

	public void testTelephone() throws Exception
	{
		process("telephone.vdm");
	}

	public void testSAFER() throws Exception
	{
		process("SAFER.vdm");
	}

	public void testExpress() throws Exception
	{
		process("express.vdm");
	}
	
	public void testStruct() throws Exception
	{
		process("struct.vdm", 3051, 3127);
	}
}
