/*******************************************************************************
 *
 *	Copyright (c) 2019 Paul Chisholm
 *
 *	Author: Paul Chisholm
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
 
package plugins.doc.definitions;

import plugins.doc.annotations.DOCAnnotationList;
import plugins.doc.definitions.DOCAccessSpecifier;
import plugins.doc.definitions.DOCDefinition;
import plugins.doc.expressions.DOCExpression;
import plugins.doc.lex.DOCNameToken;
import plugins.doc.statements.DOCErrorCaseList;
import plugins.doc.statements.DOCExternalClauseList;
import plugins.doc.statements.DOCStatement;
import plugins.doc.types.DOCPatternListTypePairList;
import plugins.doc.types.DOCPatternTypePair;

public class DOCImplicitOperationDefinition extends DOCDefinition
{
	private static final long serialVersionUID = 1L;
	private final DOCPatternListTypePairList parameterPatterns;
	private final DOCPatternTypePair result;
	private final DOCStatement body;
	private final DOCExternalClauseList externals;
	private final DOCExpression precondition;
	private final DOCExpression postcondition;
	private final DOCErrorCaseList errors;

	public DOCImplicitOperationDefinition(DOCAnnotationList annotations, DOCAccessSpecifier accessSpecifier, DOCNameToken name, DOCPatternListTypePairList parameterPatterns, DOCPatternTypePair result, DOCStatement body, DOCExternalClauseList externals, DOCExpression precondition, DOCExpression postcondition, DOCErrorCaseList errors)
	{
		super(name.location, annotations, accessSpecifier, name);
		this.parameterPatterns = parameterPatterns;
		this.result = result;
		this.body = body;
		this.externals = externals;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.errors = errors;
	}

	@Override
	public void extent(int maxWidth)
	{
		return;
	}
	
	@Override
	public String toHTML(int indent)
	{
		return null;
	}
}
