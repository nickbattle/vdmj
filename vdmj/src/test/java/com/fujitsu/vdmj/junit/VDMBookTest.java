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

package com.fujitsu.vdmj.junit;

import java.net.URL;

import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.commands.AssertCommand;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

public class VDMBookTest extends VDMTestCase
{
	private void process(String resource, String aresource) throws Exception
	{
		Console.out.println("Processing " + resource + "...");

		URL rurl = getClass().getResource("/VDMBook/" + resource);
		String file = rurl.getPath();
		URL aurl = getClass().getResource("/VDMBook/" + aresource);
		String assertions = aurl.getPath();

		long before = System.currentTimeMillis();
		ASTClassList parsed = parseClasses(file);
		long after = System.currentTimeMillis();

		Console.out.println("Parsed " + parsed.size() + " classes in " +
   			(double)(after-before)/1000 + " secs. ");

		before = System.currentTimeMillis();
		TCClassList checked = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsed);
		TypeChecker typeChecker = new ClassTypeChecker(checked);
		typeChecker.typeCheck();
		after = System.currentTimeMillis();

   		Console.out.println("Type checked in " + (double)(after-before)/1000 + " secs. ");
		Console.out.println("There were " + TypeChecker.getWarningCount() + " warnings");
		TypeChecker.printErrors(Console.out);
		assertEquals("Type check errors", 0, TypeChecker.getErrorCount());

		INClassList runnable = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checked);
		ClassInterpreter interpreter = new ClassInterpreter(runnable, checked);
		interpreter.init();
		AssertCommand cmd = new AssertCommand(new String[] {"assert", assertions});
		assertEquals("Execution errors", true, !cmd.errors());
	}

	public void test_Enigma() throws Exception
	{
		process("Enigma.vpp", "Enigma.assert");
	}

	public void test_POP3() throws Exception
	{
		process("POP3.vpp", "POP3.assert");
	}

	public void test_Factorial() throws Exception
	{
		process("factorial.vpp", "factorial.assert");
	}
}
