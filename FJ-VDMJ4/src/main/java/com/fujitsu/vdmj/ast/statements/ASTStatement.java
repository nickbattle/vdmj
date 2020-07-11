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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.statements;

import java.io.Serializable;

import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.ast.lex.LexCommentList;
import com.fujitsu.vdmj.ast.statements.visitors.ASTStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;

/**
 * The parent class of all statements.
 */
public abstract class ASTStatement extends ASTNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The location of the statement. */
	public final LexLocation location;
	/** The comments that precede the statement */
	public LexCommentList comments;

	/**
	 * Create a statement at the given location.
	 * @param location
	 */
	public ASTStatement(LexLocation location)
	{
		this.location = location;
	}

	@Override
	abstract public String toString();

	/** A string name of the statement type, (eg "if"). */
	abstract public String kind();

	public void setComments(LexCommentList comments)
	{
		this.comments = comments;
	}

	/**
	 * Implemented by all statements to allow visitor processing.
	 */
	abstract public <R, S> R apply(ASTStatementVisitor<R, S> visitor, S arg);
}
