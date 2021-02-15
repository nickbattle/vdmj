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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmjunit;

import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTBUSClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTCPUClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;

/**
 * Read a VDM++ or VDM-RT specifications.
 */
public class OOSpecificationReader extends SpecificationReader
{
	public OOSpecificationReader(Dialect dialect)
	{
		super(dialect);
	}

	/**
	 * @see com.fujitsu.vdmjunit.SpecificationReader#readSpecification(Charset, java.util.List)
	 */
	@Override
	protected Interpreter readSpecification(Charset charset, List<File> files) throws Exception
	{
		ASTClassList parsedClasses = new ASTClassList();
		
		for (File file: files)
		{
			LexTokenReader lexer = new LexTokenReader(file, Settings.dialect, charset.toString());
			ClassReader reader = new ClassReader(lexer);
			parsedClasses.addAll(reader.readClasses());
			
			if (reader.getErrorCount() > 0)
			{
				printMessages(reader.getErrors());
				fail("Syntax errors (see stdout)");
			}
		}
		
		if (Settings.dialect == Dialect.VDM_RT)
		{
			parsedClasses.add(new ASTCPUClassDefinition());
			parsedClasses.add(new ASTBUSClassDefinition());
		}
		
		TCClassList checkedClasses = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsedClasses);
		ClassTypeChecker checker = new ClassTypeChecker(checkedClasses);
		checker.typeCheck();
		
		if (ClassTypeChecker.getErrorCount() > 0)
		{
			printMessages(ClassTypeChecker.getErrors());
			fail("Type errors (see stdout)");
		}
		
		INClassList executableClasses = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checkedClasses);
		return new ClassInterpreter(executableClasses, checkedClasses);
	}
}
