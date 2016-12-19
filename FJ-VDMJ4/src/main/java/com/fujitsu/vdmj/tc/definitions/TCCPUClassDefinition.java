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

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.syntax.DefinitionReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class TCCPUClassDefinition extends TCClassDefinition
{
	private static final long serialVersionUID = 1L;

	public TCCPUClassDefinition(TCNameToken className, TCNameList supernames, TCDefinitionList definitions)
	{
		super(className, supernames, definitions);
	}

	/**
	 * This constructor is used for the virtual CPU. 
	 */
	public TCCPUClassDefinition() throws Exception
	{
		super(
			new TCNameToken(new LexLocation(), "CLASS", "CPU", false, false),
			new TCNameList(),
			operationDefs());
	}

	private static String defs =
		"operations " +
		"public CPU:(<FP>|<FCFS>) * real ==> CPU " +
		"	CPU(policy, speed) == is not yet specified; " +
		"public deploy: ? ==> () " +
		"	deploy(obj) == is not yet specified; " +
		"public deploy: ? * seq of char ==> () " +
		"	deploy(obj, name) == is not yet specified; " +
		"public setPriority: ? * nat ==> () " +
		"	setPriority(opname, priority) == is not yet specified;";

	private static TCDefinitionList operationDefs() throws Exception
	{
		LexTokenReader ltr = new LexTokenReader(defs, Dialect.VDM_PP);
		DefinitionReader dr = new DefinitionReader(ltr);
		dr.setCurrentModule("CPU");
		ASTDefinitionList ast = dr.readDefinitions();
		return ClassMapper.getInstance(TCNode.MAPPINGS).checkInit().convert(ast);	// NB. check init!!
	}
}
