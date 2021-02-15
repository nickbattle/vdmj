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

package com.fujitsu.vdmj.ast.patterns;

import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.InternalException;

public class ASTPatternBind extends ASTNode
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final ASTPattern pattern;
	public final ASTBind bind;

	public ASTPatternBind(LexLocation location, Object patternOrBind)
	{
		this.location = location;

		if (patternOrBind instanceof ASTPattern)
		{
			this.pattern = (ASTPattern)patternOrBind;
			this.bind = null;
		}
		else if (patternOrBind instanceof ASTBind)
		{
			this.pattern = null;
			this.bind = (ASTBind)patternOrBind;
		}
		else
		{
			throw new InternalException(
				3, "ASTPatternBind passed " + patternOrBind.getClass().getName());
		}
	}

	@Override
	public String toString()
	{
		return (pattern == null ? bind : pattern).toString();
	}
}
