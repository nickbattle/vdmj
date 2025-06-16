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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.minimal;

import java.io.File;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCBUSClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCCPUClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

public class MinimalRT
{
	public static void main(String[] args) throws Exception
	{
		Settings.dialect = Dialect.VDM_RT;
		File file = new File(args[0]);
		LexTokenReader ltr = new LexTokenReader(file, Dialect.VDM_RT);
		ClassReader mr = new ClassReader(ltr);
		ASTClassList classes = mr.readClasses();

		if (mr.getErrorCount() > 0)
		{
			for (VDMError error: mr.getErrors())
			{
				System.out.println(error);
			}
		}
		
		if (mr.getWarningCount() > 0)
		{
			for (VDMWarning error: mr.getWarnings())
			{
				System.out.println(error);
			}
		}

		if (mr.getErrorCount() == 0)
		{
			TCClassList tclist = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(classes);
			tclist.add(new TCCPUClassDefinition());
			tclist.add(new TCBUSClassDefinition());

    		TypeChecker tc = new ClassTypeChecker(tclist);

    		try
    		{
    			tc.typeCheck();
    		}
    		catch (Exception e)
    		{
    			System.err.println(e);
    		}

    		if (TypeChecker.getErrorCount() > 0)
    		{
    			for (VDMError error: TypeChecker.getErrors())
    			{
    				System.out.println(error);
    			}
    		}
    		
    		if (TypeChecker.getWarningCount() > 0)
    		{
    			for (VDMWarning error: TypeChecker.getWarnings())
    			{
    				System.out.println(error);
    			}
    		}
    		
    		if (TypeChecker.getErrorCount() == 0)
    		{
    			System.out.println("Typechecked.");
    		}
		}
	}
}
