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

package com.fujitsu.vdmj.in.patterns.visitors;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.patterns.INIdentifierPattern;
import com.fujitsu.vdmj.in.patterns.INPattern;

public class INFindIdentifiersVisitor extends INLeafPatternVisitor<INIdentifierPattern, List<INIdentifierPattern>, Object>
{
	public INFindIdentifiersVisitor()
	{
		// default visitorSet
	}
	
	@Override
	protected List<INIdentifierPattern> newCollection()
	{
		return new Vector<INIdentifierPattern>();
	}

	@Override
	public List<INIdentifierPattern> casePattern(INPattern node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public List<INIdentifierPattern> caseIdentifierPattern(INIdentifierPattern node, Object arg)
	{
		List<INIdentifierPattern> list = newCollection();
		list.add(node);
		return list;
	}
}
