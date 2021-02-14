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

import java.io.File;
import java.nio.charset.Charset;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import junit.framework.TestCase;

/**
 * The root of all VDMJ Unit tests.
 */

public class VDMTestCase extends TestCase
{
	protected ASTClassList parseClasses(String vpppath)
	{
		return parseClasses(vpppath, Charset.defaultCharset().name());
	}

	protected ASTClassList parseClasses(String vpppath, String charset)
	{
		Settings.release = Release.CLASSIC;
		Settings.dialect = Dialect.VDM_PP;
		ASTClassList classes = null;
		int errs = 0;
		TypeChecker.clearErrors();

		LexTokenReader ltr = new LexTokenReader(new File(vpppath), Dialect.VDM_PP, charset);
		ClassReader cr = new ClassReader(ltr);
		classes = cr.readClasses();
		cr.close();
		errs = cr.getErrorCount();
		cr.printErrors(Console.out);
		cr.printWarnings(Console.out);

		assertEquals("Syntax errors", 0, errs);
		TypeChecker.printErrors(Console.out);
		assertEquals("Type check errors", 0, TypeChecker.getErrorCount());

		return classes;
	}

	public void test()
	{
		// You have to have one to run all the JUnit tests in a package :-)
	}
}
