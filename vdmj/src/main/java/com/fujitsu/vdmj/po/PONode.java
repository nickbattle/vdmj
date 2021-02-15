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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.mapper.MappedObject;

/**
 * The abstract root of all Proof Obligation nodes. The class extends MappedObject to allow
 * the AST to be extended.
 */
abstract public class PONode extends MappedObject
{
	private static final long serialVersionUID = 1L;

	public final static String MAPPINGS = "tc-po.mappings";

	/** The textual location of the expression. */
	public final LexLocation location;

	protected PONode()
	{
		this.location = new LexLocation();	// Some nodes don't care
	}

	protected PONode(LexLocation location)
	{
		this.location = location;
	}
}
