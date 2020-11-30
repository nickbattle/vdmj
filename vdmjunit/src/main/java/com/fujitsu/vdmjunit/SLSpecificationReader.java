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
 *
 ******************************************************************************/

package com.fujitsu.vdmjunit;

import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * Read a VDM-SL specification.
 */
public class SLSpecificationReader extends SpecificationReader
{
	public SLSpecificationReader()
	{
		super(Dialect.VDM_SL);
	}

	/**
	 * @see com.fujitsu.vdmjunit.SpecificationReader#readSpecification(Charset, java.util.List)
	 */
	@Override
	protected Interpreter readSpecification(Charset charset, List<File> files) throws Exception
	{
		ASTModuleList parsedModules = new ASTModuleList();
		
		for (File file: files)
		{
			LexTokenReader lexer = new LexTokenReader(file, Settings.dialect, charset.toString());
			ModuleReader reader = new ModuleReader(lexer);
			parsedModules.addAll(reader.readModules());
			
			if (reader.getErrorCount() > 0)
			{
				printMessages(reader.getErrors());
				fail("Syntax errors (see stdout)");
			}
		}
		
		TCModuleList checkedModules = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsedModules);
		TypeChecker checker = new ModuleTypeChecker(checkedModules);
		checker.typeCheck();
		
		if (ModuleTypeChecker.getErrorCount() > 0)
		{
			printMessages(ModuleTypeChecker.getErrors());
			fail("Type errors (see stdout)");
		}
		
		INModuleList executableModules = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checkedModules);
		return new ModuleInterpreter(executableModules, checkedModules);
	}
}
