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
import com.fujitsu.vdmj.ast.lex.LexNameList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.types.ASTPatternListTypePairList;
import com.fujitsu.vdmj.ast.types.ASTPatternTypePair;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold an implicit function definition.
 */
public class ASTImplicitFunctionDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final LexNameList typeParams;
	public final ASTPatternListTypePairList parameterPatterns;
	public final ASTPatternTypePair result;
	public final ASTExpression body;
	public final ASTExpression precondition;
	public final ASTExpression postcondition;
	public final ASTExpression measureExp;

	public ASTImplicitFunctionDefinition(LexNameToken name,
		LexNameList typeParams, ASTPatternListTypePairList parameterPatterns,
		ASTPatternTypePair result,
		ASTExpression body,
		ASTExpression precondition,
		ASTExpression postcondition,
		ASTExpression measureExp)
	{
		super(name.location, name);

		this.typeParams = typeParams;
		this.parameterPatterns = parameterPatterns;
		this.result = result;
		this.body = body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.measureExp = measureExp;
	}

	@Override
	public String toString()
	{
		return	accessSpecifier + " " +	name.name +
				(typeParams == null ? "" : "[" + typeParams + "]") +
				Utils.listToString("(", parameterPatterns, ", ", ")") + result +
				(body == null ? "" : " ==\n\t" + body) +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition);
	}

	@Override
	public String kind()
	{
		return "implicit function";
	}
}
