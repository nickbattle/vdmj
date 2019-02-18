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

import plugins.doc.DOCNode;
import plugins.doc.annotations.DOCAnnotationList;
import plugins.doc.lex.DOCNameToken;

import com.fujitsu.vdmj.ast.lex.LexCommentList;
import com.fujitsu.vdmj.lex.LexLocation;

abstract public class DOCDefinition extends DOCNode
{
	private static final long serialVersionUID = 1L;
	public final LexLocation location;
	protected final DOCAccessSpecifier accessSpecifier;
	protected final DOCNameToken name;
	protected final DOCAnnotationList annotations;
	private LexCommentList comments;

	public DOCDefinition(LexLocation location, DOCAccessSpecifier accessSpecifier, DOCNameToken name)
	{
		this.location = location;
		this.accessSpecifier = accessSpecifier;
		this.name = name;
		this.annotations = null;
	}

	public DOCDefinition(LexLocation location, DOCAnnotationList annotations, DOCAccessSpecifier accessSpecifier, DOCNameToken name)
	{
		this.location = location;
		this.accessSpecifier = accessSpecifier;
		this.name = name;
		this.annotations = annotations;
	}

	public void setComments(LexCommentList comments)
	{
		this.comments = comments;
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
