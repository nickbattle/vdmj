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
import plugins.doc.patterns.DOCPatternList;
import plugins.doc.statements.DOCStatement;
import plugins.doc.types.DOCOperationType;

public class DOCExplicitOperationDefinition extends DOCDefinition
{
	private static final long serialVersionUID = 1L;
	private final DOCOperationType type;
	private final DOCPatternList parameterPatterns;
	private final DOCExpression precondition;
	private final DOCExpression postcondition;
	private final DOCStatement body;

	public DOCExplicitOperationDefinition(DOCAnnotationList annotations, DOCAccessSpecifier accessSpecifier, DOCNameToken name, DOCOperationType type, DOCPatternList parameterPatterns, DOCExpression precondition, DOCExpression postcondition, DOCStatement body)
	{
		super(name.location, annotations, accessSpecifier, name);
		this.type = type;
		this.parameterPatterns = parameterPatterns;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.body = body;
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
