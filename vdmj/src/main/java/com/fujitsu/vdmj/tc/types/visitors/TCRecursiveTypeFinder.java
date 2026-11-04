/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj.tc.types.visitors;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;

/**
 * Explore the tree of a type and indicate whether it is recursive. This is used by
 * TCType.isRecursive(). Types are recursive when a named or record type T contains T,
 * even if this is hidden behind an optional type or something that can be empty,
 * like a set/seq/map.
 * 
 * Recursive types are therefore "okay". Compare with TCInfiniteTypeFinder,
 * and INTypeSizeVisitor.
 */
public class TCRecursiveTypeFinder extends TCLeafTypeVisitor<Object, List<Object>, TCType>
{
	public boolean isRecursive()
	{
		return !done.isEmpty();
	}

	public TCTypeSet getRecursive()
	{
		return done;
	}

	@Override
	protected List<Object> newCollection()
	{
		return new Vector<Object>();
	}

	@Override
	public List<Object> caseType(TCType node, TCType arg)
	{
		return newCollection();
	}
}
