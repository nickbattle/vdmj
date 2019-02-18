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
 
package plugins.doc.statements;

import plugins.doc.expressions.DOCExpressionList;
import plugins.doc.lex.DOCIdentifierToken;
import plugins.doc.lex.DOCNameToken;
import plugins.doc.statements.DOCObjectDesignator;
import plugins.doc.statements.DOCStatement;

public class DOCCallObjectStatement extends DOCStatement
{
	private static final long serialVersionUID = 1L;
	private final DOCObjectDesignator designator;
	private final DOCNameToken classname;
	private final DOCIdentifierToken fieldname;
	private final DOCExpressionList args;

	public DOCCallObjectStatement(DOCObjectDesignator designator, DOCNameToken classname, DOCIdentifierToken fieldname, DOCExpressionList args)
	{
		super(classname.location);
		this.designator = designator;
		this.classname = classname;
		this.fieldname = fieldname;
		this.args = args;
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
