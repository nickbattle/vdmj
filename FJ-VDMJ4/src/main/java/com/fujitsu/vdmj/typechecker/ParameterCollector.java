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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.typechecker;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.tc.types.TCLeafTypeVisitor;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * This visitor produces a list of names for any parameter types
 * that are contained in the TCType being visited. This is used by the
 * TCTypeComparator. 
 */

public class ParameterCollector extends TCLeafTypeVisitor<String, Object>
{
	@Override
	public List<String> caseParameterType(TCParameterType node, Object arg)
	{
		List <String> all = new Vector<String>();
		all.add("@" + node.name);
		return all;
	}

	@Override
	public List<String> caseType(TCType node, Object arg)
	{
		return new Vector<String>();
	}
}
