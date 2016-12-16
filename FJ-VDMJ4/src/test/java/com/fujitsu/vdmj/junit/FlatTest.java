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
import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.syntax.DefinitionReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import junit.framework.TestCase;

public class FlatTest extends TestCase
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

	private void process(String resource) throws Exception
	{
		Console.out.println("Processing " + resource + "...");

		URL rurl = getClass().getResource("/flattest/" + resource);
		File file = new File(rurl.getPath());

		LexTokenReader ltr = new LexTokenReader(file, Dialect.VDM_SL);
		DefinitionReader dr = new DefinitionReader(ltr);
		ASTDefinitionList definitions = new ASTDefinitionList();

		long before = System.currentTimeMillis();
		definitions.addAll(dr.readDefinitions());
		long after = System.currentTimeMillis();
		Console.out.println("Parsed " + definitions.size() + " definitions in " +
   			(double)(after-before)/1000 + " secs. ");
		dr.printErrors(Console.out);
		dr.printWarnings(Console.out);
		assertEquals("Parse errors", 0, dr.getErrorCount());

		// TypeChecker typeChecker = new FlatTypeChecker(definitions, false);
		ASTModuleList parsed = new ASTModuleList();
		parsed.add(new ASTModule(file, definitions));
		TCModuleList checked = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsed);
		TypeChecker typeChecker = new ModuleTypeChecker(checked);

		before = System.currentTimeMillis();
		typeChecker.typeCheck();
		after = System.currentTimeMillis();
   		Console.out.println("Type checked in " + (double)(after-before)/1000 + " secs. ");
		Console.out.println("There were " + TypeChecker.getWarningCount() + " warnings");
		TypeChecker.printErrors(Console.out);
		assertEquals("Type check errors", 0, TypeChecker.getErrorCount());
	}

	public void testDFDExample() throws Exception
	{
		process("dfdexample.def");
	}

	public void testNDB() throws Exception
	{
		process("ndb.def");
	}

	public void testNewSpeak() throws Exception
	{
		process("newspeak.def");
	}

	public void testSTV() throws Exception
	{
		process("stv.def");
	}

	public void testACS() throws Exception
	{
		process("acs.def");
	}

	public void testMAA() throws Exception
	{
		process("maa.def");
	}

	public void testCrossword() throws Exception
	{
		process("crossword.def");
	}

	public void testRealm() throws Exception
	{
		process("realm.def");
	}

	public void testSort() throws Exception
	{
		process("sort.def");
	}

	public void testADT() throws Exception
	{
		process("adt.def");
	}

	public void testLibrary() throws Exception
	{
		process("library.def");
	}

	public void testPlanner() throws Exception
	{
		process("planner.def");
	}

	public void testCM() throws Exception
	{
		process("cmflat.def");
	}

	public void testWorldCup() throws Exception
	{
		process("worldcup.def");
	}

	public void testGeneral() throws Exception
	{
		process("general.def");
	}
}
