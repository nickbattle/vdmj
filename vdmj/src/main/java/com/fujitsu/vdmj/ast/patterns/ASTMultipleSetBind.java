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

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.patterns.visitors.ASTMultipleBindVisitor;

public class ASTMultipleSetBind extends ASTMultipleBind
{
	private static final long serialVersionUID = 1L;
	public final ASTExpression set;

	public ASTMultipleSetBind(ASTPatternList plist, ASTExpression set)
	{
		super(plist);
		this.set = set;
	}

	@Override
	public String toString()
	{
		return plist + " in set " + set;
	}

	@Override
	public <R, S> R apply(ASTMultipleBindVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMultipleSetBind(this, arg);
	}
}
