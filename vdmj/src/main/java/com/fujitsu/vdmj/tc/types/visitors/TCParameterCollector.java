/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * This visitor produces a list of names for any parameter types
 * that are contained in the TCType being visited. This is used by the
 * TCTypeComparator. 
 */
public class TCParameterCollector extends TCLeafTypeVisitor<TCParameterType, List<TCParameterType>, Object>
{
	public TCParameterCollector()
	{
		// No visitorSet
	}

	@Override
	public List<TCParameterType> caseParameterType(TCParameterType node, Object arg)
	{
		List <TCParameterType> all = newCollection();
		all.add(node);
		return all;
	}

	@Override
	public List<TCParameterType> caseType(TCType node, Object arg)
	{
		return newCollection();
	}

	@Override
	protected List<TCParameterType> newCollection()
	{
		return new Vector<TCParameterType>();
	}
}
