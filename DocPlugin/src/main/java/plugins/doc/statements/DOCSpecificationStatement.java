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

import plugins.doc.expressions.DOCExpression;
import plugins.doc.statements.DOCErrorCaseList;
import plugins.doc.statements.DOCExternalClauseList;
import plugins.doc.statements.DOCStatement;

import com.fujitsu.vdmj.lex.LexLocation;

public class DOCSpecificationStatement extends DOCStatement
{
	private final DOCExternalClauseList externals;
	private final DOCExpression precondition;
	private final DOCExpression postcondition;
	private final DOCErrorCaseList errors;

	public DOCSpecificationStatement(LexLocation location, DOCExternalClauseList externals, DOCExpression precondition, DOCExpression postcondition, DOCErrorCaseList errors)
	{
		super(location);
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
