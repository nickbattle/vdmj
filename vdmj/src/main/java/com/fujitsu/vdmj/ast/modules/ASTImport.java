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

package com.fujitsu.vdmj.ast.modules;

import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.lex.LexLocation;

/**
 * The parent class of all import declarations.
 */
abstract public class ASTImport extends ASTNode
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the import declaration. */
	public final LexLocation location;
	/** The name of the object being imported. */
	public final LexNameToken name;
	/** The renamed name of the object being imported. */
	public final LexNameToken renamed;
	/** The TCModule imported from. */
	public ASTModule from;

	/**
	 * Create an import declaration for the given name(s).
	 */
	public ASTImport(LexNameToken name, LexNameToken renamed)
	{
		this.location = name.location;
		this.name = name;
		this.renamed = renamed;
	}

	@Override
	abstract public String toString();
}
