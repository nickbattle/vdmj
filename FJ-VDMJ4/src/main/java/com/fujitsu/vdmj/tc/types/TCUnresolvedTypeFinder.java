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
		if (node.type instanceof TCUnresolvedType)	// N = Z
		{
			return new TCTypeList(node.type);
		}
		else if (node.type instanceof TCUnionType)	// N = A | B | C
		{
			TCUnionType union = (TCUnionType)node.type;
			TCTypeList all = newCollection();
			
			for (TCType type: union.types)
			{
				if (type instanceof TCUnresolvedType)
				{
					all.add(type);
				}
			}
			
			return all;
		}
		else
		{
			return newCollection();
		}
	}
	
	@Override
	public TCTypeList caseRecordType(TCRecordType node, Object arg)
	{
		TCTypeList all = newCollection();
		
		for (TCField field: node.fields)
		{
			if (field.type instanceof TCUnresolvedType)
			{
				all.add(field.type);	// Don't recurse
			}
		}
		
		return all;
	}
	
	@Override
	public TCTypeList caseType(TCType node, Object arg)
	{
		return newCollection();
	}
}
