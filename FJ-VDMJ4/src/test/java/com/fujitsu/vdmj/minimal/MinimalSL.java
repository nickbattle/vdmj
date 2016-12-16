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

package com.fujitsu.vdmj.minimal;

import java.io.File;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

public class MinimalSL
{
	public static void main(String[] args) throws Exception
	{
		Settings.dialect = Dialect.VDM_SL;
		File file = new File(args[0]);
		LexTokenReader ltr = new LexTokenReader(file, Dialect.VDM_SL);
		ModuleReader mr = new ModuleReader(ltr);
		ASTModuleList modules = mr.readModules();

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

			TCModuleList tclist = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(modules);
			tclist.combineDefaults();
    		TypeChecker tc = new ModuleTypeChecker(tclist);

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
