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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCMapletExpression extends TCNode
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final TCExpression left;
	public final TCExpression right;

	public TCMapletExpression(TCExpression left, TCExpression right)
	{
		this.location = left.location;
		this.left = left;
		this.right = right;
	}

	public TCType typeCheck(Environment env, NameScope scope, TCType domConstraint, TCType rngConstraint)
	{
		TCType ltype = left.typeCheck(env, null, scope, domConstraint);
		TCType rtype = right.typeCheck(env, null, scope, rngConstraint);

		return new TCMapType(location, ltype, rtype);
	}

	@Override
	public String toString()
	{
		return left + " |-> " + right;
	}
}
