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

import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.lex.LexLocation;

/**
 * A comment within the spec. This isn't part of the grammar, as such.
 */
public class LexComment extends ASTNode
{
	private static final long serialVersionUID = 1L;
	
	public final LexLocation location;
	public final String comment;
	public final boolean block;
	public final LexLocation endloc;	// Last line of multi-line merged comment

	public LexComment(LexLocation location, String comment, boolean block, LexLocation endloc)
	{
		super();
		
		this.location = location;
		this.comment = comment;
		this.block = block;
		this.endloc = endloc;
	}

	public LexComment(LexLocation location, String comment, boolean block)
	{
		this(location, comment, block, location);
	}

	@Override
	public String toString()
	{
		return block ? "/*" + comment + "*/" : "--" + comment;
	}
}
