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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.lex;

import java.util.Vector;

import com.fujitsu.vdmj.config.Properties;
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
		LexLocation endloc = location;
		
		if (size() > 0 && Properties.parser_merge_comments)
		{
			LexComment previous = this.lastElement();
			
			/**
			 * We merge this comment into the previous one if neither is a block comment,
			 * if the previous one is on the line above, and if the current comment does not
			 * contain what looks like the start of an annotation. 
			 */
			
			if (!previous.block && !block &&
				 previous.endloc.startLine == location.startLine - 1 &&
				!comment.trim().startsWith("@"))
			{
				this.remove(size() - 1);
				
				// Simulate a block comment, including the indentation padding
				comment = previous.comment + "\n" + padding(location.startPos - 1) + comment;
				endloc = location;				// New end is this comment
				location = previous.location;	// Start of growing block
			}
		}

		this.add(new LexComment(location, comment, block, endloc));
	}

	public String comment(int i)
	{
		return get(i).comment;
	}

	public LexLocation location(int i)
	{
		return get(i).location;
	}
	
	private String padding(int len)
	{
		StringBuilder sb = new StringBuilder();
		
		for (int i=0; i<len; i++)
		{
			sb.append(' ');
		}
		
		return sb.toString();
	}
}
