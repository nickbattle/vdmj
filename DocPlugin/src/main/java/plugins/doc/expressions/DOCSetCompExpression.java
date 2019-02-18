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

import plugins.doc.expressions.DOCExpression;
import plugins.doc.expressions.DOCSetExpression;
import plugins.doc.patterns.DOCMultipleBindList;

import com.fujitsu.vdmj.lex.LexLocation;

public class DOCSetCompExpression extends DOCSetExpression
{
	private final DOCExpression first;
	private final DOCMultipleBindList bindings;
	private final DOCExpression predicate;

	public DOCSetCompExpression(LexLocation location, DOCExpression first, DOCMultipleBindList bindings, DOCExpression predicate)
	{
		super(location);
		this.first = first;
		this.bindings = bindings;
		this.predicate = predicate;
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
