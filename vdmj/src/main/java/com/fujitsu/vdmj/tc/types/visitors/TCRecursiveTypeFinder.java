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

import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * Explore the tree of a type and indicate whether it is recursive, by returning a
 * non-empty set of bools. This is used by TCType.isRecursive(). Types are recursive
 * when a named or record type T contains T, even if this is hidden behind an optional
 * type or something that can be empty, like a set/seq/map.
 * 
 * Recursive types are therefore "okay". Compare with TCInfiniteTypeFinder,
 * and INTypeSizeVisitor.
 */
public class TCRecursiveTypeFinder extends TCLeafTypeVisitor<Boolean, Set<Boolean>, TCType>
{
	@Override
	protected Set<Boolean> newCollection()
	{
		return new HashSet<Boolean>();
	}

	@Override
	public Set<Boolean> caseType(TCType node, TCType arg)
	{
		return newCollection();
	}

	@Override
	public Set<Boolean> caseNamedType(TCNamedType node, TCType arg)
	{
		if (done.contains(node) && node.equals(arg))
		{
			return trueSet();
		}
		else
		{
			return super.caseNamedType(node, arg);
		}
	}
	
	@Override
	public Set<Boolean> caseRecordType(TCRecordType node, TCType arg)
	{
		if (done.contains(node) && node.equals(arg))
		{
			return trueSet();
		}
		else
		{
			return super.caseRecordType(node, arg);
		}
	}

	private Set<Boolean> trueSet()
	{
		Set<Boolean> result = newCollection();
		result.add(true);
		return result;
	}
}
