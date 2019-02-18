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

import plugins.doc.patterns.DOCBind;

import com.fujitsu.vdmj.lex.LexLocation;

public class DOCSeqCompExpression extends DOCSeqExpression
{
	private static final long serialVersionUID = 1L;
	private final DOCExpression first;
	private final DOCBind bind;
	private final DOCExpression predicate;

	public DOCSeqCompExpression(LexLocation location, DOCExpression first, DOCBind bind, DOCExpression predicate)
	{
		super(location);
		this.first = first;
		this.bind = bind;
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
