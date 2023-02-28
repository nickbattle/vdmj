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

package com.fujitsu.vdmj.po.patterns;

import com.fujitsu.vdmj.po.patterns.visitors.POPatternVisitor;

public class POMapletPattern extends POPattern
{
	private static final long serialVersionUID = 1L;
	public final POPattern from;
	public final POPattern to;

	public POMapletPattern(POPattern from, POPattern to)
	{
		super(from.location);
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString()
	{
		return from + " |-> " + to;
	}

	@Override
	public boolean isSimple()
	{
		return from.isSimple() && to.isSimple();
	}

	@Override
	public <R, S> R apply(POPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapletPattern(this, arg);
	}
}
