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

package com.fujitsu.vdmj.tc.patterns;

import com.fujitsu.vdmj.tc.expressions.TCExpression;

public class TCSeqBind extends TCBind
{
	private static final long serialVersionUID = 1L;
	public final TCExpression sequence;

	public TCSeqBind(TCPattern pattern, TCExpression sequence)
	{
		super(pattern.location, pattern);
		this.sequence = sequence;
	}

	@Override
	public TCMultipleBindList getMultipleBindList()
	{
		TCPatternList plist = new TCPatternList();
		plist.add(pattern);
		TCMultipleBindList mblist = new TCMultipleBindList();
		mblist.add(new TCMultipleSeqBind(plist, sequence));
		return mblist;
	}

	@Override
	public String toString()
	{
		return pattern + " in seq " + sequence;
	}

	@Override
	public <R, S> R apply(TCBindVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSeqBind(this, arg);
	}
}
