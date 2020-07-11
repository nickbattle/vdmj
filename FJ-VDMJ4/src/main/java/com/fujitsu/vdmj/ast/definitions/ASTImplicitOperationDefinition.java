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

import com.fujitsu.vdmj.ast.definitions.visitors.ASTDefinitionVisitor;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.statements.ASTErrorCaseList;
import com.fujitsu.vdmj.ast.statements.ASTExternalClauseList;
import com.fujitsu.vdmj.ast.statements.ASTSpecificationStatement;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.ast.types.ASTPatternListTypePairList;
import com.fujitsu.vdmj.ast.types.ASTPatternTypePair;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold an explicit operation definition.
 */
public class ASTImplicitOperationDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final ASTPatternListTypePairList parameterPatterns;
	public final ASTPatternTypePair result;
	public final ASTExternalClauseList externals;
	public final ASTStatement body;
	public final ASTExpression precondition;
	public final ASTExpression postcondition;
	public final ASTErrorCaseList errors;

	public ASTImplicitOperationDefinition(LexNameToken name,
		ASTPatternListTypePairList parameterPatterns,
		ASTPatternTypePair result, ASTStatement body,
		ASTSpecificationStatement spec)
	{
		super(name.location, name);

		this.parameterPatterns = parameterPatterns;
		this.result = result;
		this.body = body;
		this.externals = spec.externals;
		this.precondition = spec.precondition;
		this.postcondition = spec.postcondition;
		this.errors = spec.errors;
	}

	@Override
	public String toString()
	{
		return	(accessSpecifier.isPure ? "pure " : "") + name +
				Utils.listToString("(", parameterPatterns, ", ", ")") +
				(result == null ? "" : " " + result) +
				(externals == null ? "" : "\n\text " + externals) +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition) +
				(errors == null ? "" : "\n\terrs " + errors);
	}

	@Override
	public String kind()
	{
		return "implicit operation";
	}

	@Override
	public <R, S> R apply(ASTDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseImplicitOperationDefinition(this, arg);
	}
}
