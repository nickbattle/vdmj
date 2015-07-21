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

package vdmjunit;

import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.overturetool.vdmj.Settings;
import org.overturetool.vdmj.definitions.BUSClassDefinition;
import org.overturetool.vdmj.definitions.CPUClassDefinition;
import org.overturetool.vdmj.definitions.ClassList;
import org.overturetool.vdmj.lex.Dialect;
import org.overturetool.vdmj.lex.LexTokenReader;
import org.overturetool.vdmj.runtime.ClassInterpreter;
import org.overturetool.vdmj.runtime.Interpreter;
import org.overturetool.vdmj.syntax.ClassReader;
import org.overturetool.vdmj.typechecker.ClassTypeChecker;

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
	 * @see vdmjunit.SpecificationReader#readSpecification(Charset, java.util.List)
	 */
	@Override
	protected Interpreter readSpecification(Charset charset, List<File> files) throws Exception
	{
		ClassList classes = new ClassList();
		
		for (File file: files)
		{
			LexTokenReader lexer = new LexTokenReader(file, Settings.dialect, charset.toString());
			ClassReader reader = new ClassReader(lexer);
			classes.addAll(reader.readClasses());
			
			if (reader.getErrorCount() > 0)
			{
				printMessages(reader.getErrors());
				fail("Syntax errors (see stdout)");
			}
		}
		
		if (Settings.dialect == Dialect.VDM_RT)
		{
			classes.add(new CPUClassDefinition());
  			classes.add(new BUSClassDefinition());
		}
		
		ClassTypeChecker checker = new ClassTypeChecker(classes);
		checker.typeCheck();
		
		if (ClassTypeChecker.getErrorCount() > 0)
		{
			printMessages(ClassTypeChecker.getErrors());
			fail("Type errors (see stdout)");
		}
		
		return new ClassInterpreter(classes);
	}
}
