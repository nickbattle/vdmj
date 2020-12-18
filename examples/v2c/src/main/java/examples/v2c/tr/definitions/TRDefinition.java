/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package examples.v2c.tr.definitions;

import com.fujitsu.vdmj.ast.lex.LexComment;
import com.fujitsu.vdmj.ast.lex.LexCommentList;

import examples.v2c.tr.TRNode;

public abstract class TRDefinition extends TRNode
{
	private static final long serialVersionUID = 1L;
	protected final LexCommentList comments;
	
	protected TRDefinition(LexCommentList comments)
	{
		this.comments = comments;
	}
	
	public String translate()
	{
		StringBuilder sb = new StringBuilder();
		
		for (LexComment c: comments)
		{
			if (c.block)
			{
				sb.append(c.toString());
			}
			else
			{
				sb.append("//");
				sb.append(c.comment);
			}
			
			sb.append("\n");
		}

		return sb.toString();
	}
}
