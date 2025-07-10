/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package com.fujitsu.vdmj.po.annotations;

import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.tc.annotations.TCLoopInvariantAnnotation;
import com.fujitsu.vdmj.tc.annotations.TCLoopInvariantList;

public class POLoopInvariantList extends POMappedList<TCLoopInvariantAnnotation, POLoopInvariantAnnotation>
{
	private final POAssignmentDefinition ghostDef;
	
	public POLoopInvariantList(TCLoopInvariantList from, POAssignmentDefinition ghostDef) throws Exception
	{
		super(from);
		this.ghostDef = ghostDef;
	}

	public POAssignmentDefinition getGhostDef()
	{
		return ghostDef;
	}
}
