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
import plugins.doc.patterns.DOCPattern;
import plugins.doc.types.DOCType;

public class DOCValueDefinition extends DOCDefinition
{
	private static final long serialVersionUID = 1L;
	private final DOCPattern pattern;
	private final DOCType type;
	private final DOCExpression exp;

	public DOCValueDefinition(DOCAnnotationList annotations, DOCAccessSpecifier accessSpecifier, DOCPattern pattern, DOCType type, DOCExpression exp)
	{
		super(pattern.location, annotations, accessSpecifier, null);
		this.pattern = pattern;
		this.type = type;
		this.exp = exp;
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
