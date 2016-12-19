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

import java.net.URL;

import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

public class CharsetTest extends VDMTestCase
{
	private void process(String resource, String charset) throws Exception
	{
		Console.out.println("Processing " + resource + "...");

		URL rurl = getClass().getResource("/charsets/" + resource);
		String file = rurl.getPath();

		long before = System.currentTimeMillis();
		ASTClassList parsed = parseClasses(file, charset);
		TCClassList classes = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsed);
		long after = System.currentTimeMillis();

		Console.out.println("Parsed " + classes.size() + " classes in " +
   			(double)(after-before)/1000 + " secs. ");

		before = System.currentTimeMillis();
		TypeChecker typeChecker = new ClassTypeChecker(classes);
		typeChecker.typeCheck();
		after = System.currentTimeMillis();

   		Console.out.println("Type checked in " + (double)(after-before)/1000 + " secs. ");
		Console.out.println("There were " + TypeChecker.getWarningCount() + " warnings");
		assertEquals("Type check errors", 0, TypeChecker.getErrorCount());
	}

	public void test_Dvorak() throws Exception
	{
		process("Dvorak.vpp", "UTF-8");
	}

	public void test_JapaneseUtf8() throws Exception
	{
		process("Japanese_UTF8.vpp", "UTF-8");
	}

	public void test_Shift_JIS() throws Exception
	{
		process("Shift_JIS.vpp", "SJIS");
	}
}
