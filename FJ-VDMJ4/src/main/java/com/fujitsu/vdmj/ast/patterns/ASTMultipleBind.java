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

package com.fujitsu.vdmj.ast.patterns;

import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;

/**
 * The parent class of {@link TCMultipleSetBind} and {@link TCMultipleTypeBind}.
 */
public abstract class ASTMultipleBind extends ASTNode
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the bind. */
	public final LexLocation location;
	/** The list of patterns for this bind. */
	public final ASTPatternList plist;

	/**
	 * Create a multiple bind with the given pattern list. The location is
	 * taken from the location of the first pattern in the list.
	 */
	public ASTMultipleBind(ASTPatternList plist)
	{
		this.plist = plist;
		this.location = plist.get(0).location;
	}

	/** Return this one bind as a list of {@link TCMultipleBind}. */
	public ASTMultipleBindList getMultipleBindList()
	{
		ASTMultipleBindList list = new ASTMultipleBindList();
		list.add(this);
		return list;
	}
}
