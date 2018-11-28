/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
 *
 *	Author: Nick Battle
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

package com.fujitsu.vdmj.ast.lex;

import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;

public class LexCommentList extends Vector<LexComment>
{
	private static final long serialVersionUID = 1L;

	public LexCommentList(LexCommentList comments)
	{
		this.addAll(comments);
	}

	public LexCommentList()
	{
		super();
	}
	
	public void add(LexLocation location, String comment, boolean block)
	{
		this.add(new LexComment(location, comment, block));
	}

	public String comment(int i)
	{
		return get(i).comment;
	}

	public LexLocation location(int i)
	{
		return get(i).location;
	}
}
