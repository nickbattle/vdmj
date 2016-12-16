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

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.patterns.ASTPatternList;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.ast.types.ASTOperationType;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold an explicit operation definition.
 */
public class ASTExplicitOperationDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final ASTOperationType type;
	public final ASTPatternList parameterPatterns;
	public final ASTExpression precondition;
	public final ASTExpression postcondition;
	public final ASTStatement body;

	public ASTExplicitOperationDefinition(LexNameToken name, ASTOperationType type,
		ASTPatternList parameters, ASTExpression precondition,
		ASTExpression postcondition, ASTStatement body)
	{
		super(name.location, name);

		this.type = type;
		this.parameterPatterns = parameters;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.body = body;
	}

	@Override
	public String toString()
	{
		return  (type.isPure() ? "pure " : "") + name + ": " + type +
				"\n\t" + name + "(" + Utils.listToString(parameterPatterns) + ")" +
				(body == null ? "" : " ==\n" + body) +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition);
	}
	
	@Override
	public void setAccessSpecifier(ASTAccessSpecifier access)
	{
		super.setAccessSpecifier(access);
		type.setPure(access.isPure);
	}

	@Override
	public String kind()
	{
		return "explicit operation";
	}
}
