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

package com.fujitsu.vdmj.ast;

import com.fujitsu.vdmj.lex.LexLocation;

/**
 * A comment within the spec. This isn't part of the grammar, as such.
 */
public class ASTComment extends ASTNode
{
	private static final long serialVersionUID = 1L;
	
	public final LexLocation location;
	public final String comment;

	public ASTComment(LexLocation location, String comment)
	{
		super();
		
		this.location = location;
		this.comment = comment;
	}
	
	public String toString()
	{
		return "/*" + comment + "*/";
	}
}
