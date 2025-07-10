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

package com.fujitsu.vdmj.in.annotations;

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.in.definitions.INAssignmentDefinition;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.annotations.TCLoopInvariantAnnotation;
import com.fujitsu.vdmj.tc.annotations.TCLoopInvariantList;
import com.fujitsu.vdmj.values.NameValuePairList;

public class INLoopInvariantList extends INMappedList<TCLoopInvariantAnnotation, INLoopInvariantAnnotation>
{
	private final INAssignmentDefinition ghostDef;
	
	public INLoopInvariantList(TCLoopInvariantList from, INAssignmentDefinition ghostDef) throws Exception
	{
		super(from);
		this.ghostDef = ghostDef;
	}

	public INAssignmentDefinition getGhostDef()
	{
		return ghostDef;
	}

	public NameValuePairList getGhostValue(Context ctxt)
	{
		if (ghostDef == null)
		{
			return new NameValuePairList();
		}
		else
		{
			return ghostDef.getNamedValues(ctxt);
		}
	}
}
