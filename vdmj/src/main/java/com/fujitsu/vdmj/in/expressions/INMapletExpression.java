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

package com.fujitsu.vdmj.in.expressions;

import java.io.Serializable;

import com.fujitsu.vdmj.in.INNode;

public class INMapletExpression extends INNode
{
	private static final long serialVersionUID = 1L;

	public final INExpression left;
	public final INExpression right;

	public INMapletExpression(INExpression left, INExpression right)
	{
		super(left.location);
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString()
	{
		return left + " |-> " + right;
	}
}
