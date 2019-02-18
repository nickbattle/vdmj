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
import plugins.doc.patterns.DOCPattern;
import plugins.doc.types.DOCInvariantType;

public class DOCTypeDefinition extends DOCDefinition
{
	private static final long serialVersionUID = 1L;
	private final DOCInvariantType type;
	private final DOCPattern invPattern;
	private final DOCExpression invExpression;
	private final DOCPattern eqPattern1;
	private final DOCPattern eqPattern2;
	private final DOCExpression eqExpression;
	private final DOCPattern ordPattern1;
	private final DOCPattern ordPattern2;
	private final DOCExpression ordExpression;

	public DOCTypeDefinition(DOCAnnotationList annotations, DOCAccessSpecifier accessSpecifier, DOCNameToken name, DOCInvariantType type, DOCPattern invPattern, DOCExpression invExpression, DOCPattern eqPattern1, DOCPattern eqPattern2, DOCExpression eqExpression, DOCPattern ordPattern1, DOCPattern ordPattern2, DOCExpression ordExpression)
	{
		super(name.location, annotations, accessSpecifier, name);
		this.type = type;
		this.invPattern = invPattern;
		this.invExpression = invExpression;
		this.eqPattern1 = eqPattern1;
		this.eqPattern2 = eqPattern2;
		this.eqExpression = eqExpression;
		this.ordPattern1 = ordPattern1;
		this.ordPattern2 = ordPattern2;
		this.ordExpression = ordExpression;
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
