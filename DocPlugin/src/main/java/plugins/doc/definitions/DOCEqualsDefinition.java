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
import plugins.doc.patterns.DOCBind;
import plugins.doc.patterns.DOCPattern;
import plugins.doc.patterns.DOCTypeBind;

import com.fujitsu.vdmj.lex.LexLocation;

public class DOCEqualsDefinition extends DOCDefinition
{
	private static final long serialVersionUID = 1L;
	private final DOCPattern pattern;
	private final DOCTypeBind typebind;
	private final DOCBind bind;
	private final DOCExpression test;

	public DOCEqualsDefinition(LexLocation location, DOCPattern pattern, DOCTypeBind typebind, DOCBind bind, DOCExpression test)
	{
		super(location, null, null);
		this.pattern = pattern;
		this.typebind = typebind;
		this.bind = bind;
		this.test = test;
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
