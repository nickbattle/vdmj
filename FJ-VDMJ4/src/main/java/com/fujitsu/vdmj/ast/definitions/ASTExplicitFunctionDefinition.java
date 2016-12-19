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
import com.fujitsu.vdmj.ast.patterns.ASTPatternList;
import com.fujitsu.vdmj.ast.patterns.ASTPatternListList;
import com.fujitsu.vdmj.ast.types.ASTFunctionType;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold an explicit function definition.
 */
public class ASTExplicitFunctionDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final LexNameList typeParams;
	public final ASTFunctionType type;
	public final ASTPatternListList paramPatternList;
	public final ASTExpression precondition;
	public final ASTExpression postcondition;
	public final ASTExpression body;
	public final boolean isTypeInvariant;
	public final LexNameToken measure;

	public ASTExplicitFunctionDefinition(LexNameToken name, LexNameList typeParams,
		ASTFunctionType type, ASTPatternListList parameters,
		ASTExpression body,
		ASTExpression precondition, ASTExpression postcondition, boolean typeInvariant,
		LexNameToken measure)
	{
		super(name.location, name);

		this.typeParams = typeParams;
		this.type = type;
		this.paramPatternList = parameters;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.body = body;
		this.isTypeInvariant = typeInvariant;
		this.measure = measure;
	}

	@Override
	public String toString()
	{
		StringBuilder params = new StringBuilder();

		for (ASTPatternList plist: paramPatternList)
		{
			params.append("(" + Utils.listToString(plist) + ")");
		}

		return accessSpecifier.ifSet(" ") + name.name +
				(typeParams == null ? ": " : "[" + typeParams + "]: ") + type +
				"\n\t" + name.name + params + " ==\n" + body +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition);
	}

	@Override
	public String kind()
	{
		return "explicit function";
	}
}
