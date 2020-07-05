/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.tc.TCVisitorSet;

public class TCUnresolvedTypeFinder extends TCLeafTypeVisitor<TCType, TCTypeList, Object>
{
	protected TCUnresolvedTypeFinder(TCVisitorSet<TCType, TCTypeList, Object> visitors)
	{
		super(visitors);
	}

	public TCUnresolvedTypeFinder()
	{
		super(null);
	}

	@Override
	protected TCTypeList newCollection()
	{
		return new TCTypeList();
	}
	
	@Override
	public TCTypeList caseUnresolvedType(TCUnresolvedType node, Object arg)
	{
		return new TCTypeList(node);
	}

	/**
	 * To avoid infinite type loops, we stop searching at "named" types and records.
	 */
	@Override
	public TCTypeList caseNamedType(TCNamedType node, Object arg)
	{
		if (done.contains(node))
		{
			return newCollection();
		}
		else
		{
			done.add(node);
			return super.caseNamedType(node, arg);
		}
	}
	
	@Override
	public TCTypeList caseRecordType(TCRecordType node, Object arg)
	{
		if (done.contains(node))
		{
			return newCollection();
		}
		else
		{
			done.add(node);
			return super.caseRecordType(node, arg);
		}
	}
	
	@Override
	public TCTypeList caseType(TCType node, Object arg)
	{
		return newCollection();
	}
}
