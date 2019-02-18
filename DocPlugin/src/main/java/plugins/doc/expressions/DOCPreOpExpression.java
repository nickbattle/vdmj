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
 
package plugins.doc.expressions;

import plugins.doc.definitions.DOCStateDefinition;
import plugins.doc.expressions.DOCExpression;
import plugins.doc.lex.DOCNameToken;
import plugins.doc.statements.DOCErrorCaseList;

public class DOCPreOpExpression extends DOCExpression
{
	private static final long serialVersionUID = 1L;
	private final DOCNameToken opname;
	private final DOCExpression expression;
	private final DOCErrorCaseList errors;
	private final DOCStateDefinition state;

	public DOCPreOpExpression(DOCNameToken opname, DOCExpression expression, DOCErrorCaseList errors, DOCStateDefinition state)
	{
		super(opname.location);
		this.opname = opname;
		this.expression = expression;
		this.errors = errors;
		this.state = state;
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
