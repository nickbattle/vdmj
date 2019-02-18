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
import plugins.doc.patterns.DOCPatternBind;
import plugins.doc.statements.DOCStatement;

import com.fujitsu.vdmj.lex.LexLocation;

public class DOCForPatternBindStatement extends DOCStatement
{
	private final DOCPatternBind patternBind;
	private final boolean reverse;
	private final DOCExpression exp;
	private final DOCStatement statement;

	public DOCForPatternBindStatement(LexLocation location, DOCPatternBind patternBind, boolean reverse, DOCExpression exp, DOCStatement statement)
	{
		super(location);
		this.patternBind = patternBind;
		this.reverse = reverse;
		this.exp = exp;
		this.statement = statement;
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
