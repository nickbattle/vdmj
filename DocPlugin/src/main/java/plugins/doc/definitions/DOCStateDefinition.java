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

import plugins.doc.definitions.DOCDefinition;
import plugins.doc.expressions.DOCExpression;
import plugins.doc.lex.DOCNameToken;
import plugins.doc.patterns.DOCPattern;
import plugins.doc.types.DOCFieldList;

public class DOCStateDefinition extends DOCDefinition
{
	private static final long serialVersionUID = 1L;
	private final DOCFieldList fields;
	private final DOCPattern invPattern;
	private final DOCExpression invExpression;
	private final DOCPattern initPattern;
	private final DOCExpression initExpression;

	public DOCStateDefinition(DOCNameToken name, DOCFieldList fields, DOCPattern invPattern, DOCExpression invExpression, DOCPattern initPattern, DOCExpression initExpression)
	{
		super(name.location, DOCAccessSpecifier.DEFAULT, name);
		this.fields = fields;
		this.invPattern = invPattern;
		this.invExpression = invExpression;
		this.initPattern = initPattern;
		this.initExpression = initExpression;
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
