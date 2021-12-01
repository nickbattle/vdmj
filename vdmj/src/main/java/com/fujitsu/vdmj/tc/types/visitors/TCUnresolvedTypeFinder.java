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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.types.visitors;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;

public class TCUnresolvedTypeFinder extends TCLeafTypeVisitor<TCType, TCTypeList, Object>
{
	public TCUnresolvedTypeFinder()
	{
		visitorSet = new TCVisitorSet<TCType, TCTypeList, Object>()
		{
			@Override
			protected void setVisitors()
			{
				// None
			}

			@Override
			protected TCTypeList newCollection()
			{
				return null;
			}
		};
	}

	@Override
	protected TCTypeList newCollection()
	{
		return new TCTypeList();
	}
	
	@Override
	public TCTypeList caseParameterType(TCParameterType node, Object arg)
	{
		return new TCTypeList(node);
	}

	@Override
	public TCTypeList caseUnresolvedType(TCUnresolvedType node, Object arg)
	{
		return new TCTypeList(node);
	}

	@Override
	public TCTypeList caseType(TCType node, Object arg)
	{
		return newCollection();
	}
}
