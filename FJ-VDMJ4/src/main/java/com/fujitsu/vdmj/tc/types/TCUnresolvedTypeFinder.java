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

public class TCUnresolvedTypeFinder extends TCLeafTypeVisitor<TCType, TCTypeList, Object>
{
	/**
	 * To prevent recursive cases, where a type refers to itself, we record possible
	 * recursive roots here, and test before recursing again. See below.
	 */
	private TCTypeSet recursiveSeen = new TCTypeSet();
	
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
		if (recursiveSeen.contains(node))
		{
			return newCollection();
		}
		else
		{
			recursiveSeen.add(node);
			return super.caseNamedType(node, arg);
		}
	}
	
	@Override
	public TCTypeList caseRecordType(TCRecordType node, Object arg)
	{
		if (recursiveSeen.contains(node))
		{
			return newCollection();
		}
		else
		{
			recursiveSeen.add(node);
			return super.caseRecordType(node, arg);
		}
	}
	
	@Override
	public TCTypeList caseType(TCType node, Object arg)
	{
		return newCollection();
	}
}
