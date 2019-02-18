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
import plugins.doc.lex.DOCNameList;
import plugins.doc.lex.DOCNameToken;
import plugins.doc.patterns.DOCPatternListList;
import plugins.doc.types.DOCFunctionType;

public class DOCExplicitFunctionDefinition extends DOCDefinition
{
	private static final long serialVersionUID = 1L;
	private final DOCNameList typeParams;
	private final DOCFunctionType type;
	private final DOCPatternListList paramPatternList;
	private final DOCExpression body;
	private final DOCExpression precondition;
	private final DOCExpression postcondition;
	private final boolean isTypeInvariant;
	private final DOCExpression measure;

	public DOCExplicitFunctionDefinition(DOCAnnotationList annotations, DOCAccessSpecifier accessSpecifier, DOCNameToken name, DOCNameList typeParams, DOCFunctionType type, DOCPatternListList paramPatternList, DOCExpression body, DOCExpression precondition, DOCExpression postcondition, boolean isTypeInvariant, DOCExpression measure)
	{
		super(name.location, annotations, accessSpecifier, name);
		this.typeParams = typeParams;
		this.type = type;
		this.paramPatternList = paramPatternList;
		this.body = body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.isTypeInvariant = isTypeInvariant;
		this.measure = measure;
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
