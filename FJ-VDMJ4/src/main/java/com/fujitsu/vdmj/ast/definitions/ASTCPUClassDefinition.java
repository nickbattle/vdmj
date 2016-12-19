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

package com.fujitsu.vdmj.ast.definitions;

import com.fujitsu.vdmj.ast.lex.LexNameList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.syntax.DefinitionReader;
import com.fujitsu.vdmj.syntax.ParserException;

public class ASTCPUClassDefinition extends ASTClassDefinition
{
	private static final long serialVersionUID = 1L;

	public ASTCPUClassDefinition() throws ParserException, LexException
	{
		super(
			new LexNameToken("CLASS", "CPU", new LexLocation()),
			new LexNameList(),
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

	private static ASTDefinitionList operationDefs() throws ParserException, LexException
	{
		LexTokenReader ltr = new LexTokenReader(defs, Dialect.VDM_PP);
		DefinitionReader dr = new DefinitionReader(ltr);
		dr.setCurrentModule("CPU");
		return dr.readDefinitions();
	}
}
