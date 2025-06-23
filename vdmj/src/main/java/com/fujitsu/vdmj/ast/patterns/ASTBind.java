/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.ast.patterns;

import java.io.Serializable;
import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.ast.patterns.visitors.ASTBindVisitor;
import com.fujitsu.vdmj.lex.LexLocation;

/**
 * The parent class of {@link ASTSetBind} and {@link ASTTypeBind}.
 */
public abstract class ASTBind extends ASTNode
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the bind. */
	public final LexLocation location;
	/** The pattern of the bind. */
	public final ASTPattern pattern;

	/**
	 * Create a bind at the given location with the given pattern.
	 */
	public ASTBind(LexLocation location, ASTPattern pattern)
	{
		this.location = location;
		this.pattern = pattern;
	}

	/**
	 * Implemented by all binds to allow visitor processing.
	 */
	abstract public <R, S> R apply(ASTBindVisitor<R, S> visitor, S arg);
}
