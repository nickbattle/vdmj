/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.po.traces;

import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.tc.traces.TCTraceDefinitionTerm;
import com.fujitsu.vdmj.tc.traces.TCTraceDefinitionTermList;

/**
 * A class representing a sequence of trace definitions.
 */
public class POTraceDefinitionTermList extends POMappedList<TCTraceDefinitionTerm, POTraceDefinitionTerm>
{
	public POTraceDefinitionTermList()
	{
		super();
	}

	public POTraceDefinitionTermList(TCTraceDefinitionTermList from) throws Exception
	{
		super(from);
	}
}
