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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.traces;

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.traces.TCTraceDefinition;
import com.fujitsu.vdmj.tc.traces.TCTraceDefinitionTerm;
import com.fujitsu.vdmj.traces.TraceIterator;
import com.fujitsu.vdmj.traces.TraceIteratorList;

/**
 * A class representing a sequence of trace definitions.
 */
@SuppressWarnings("serial")
public class INTraceDefinitionTerm extends INMappedList<TCTraceDefinition, INTraceDefinition>
{
	public INTraceDefinitionTerm()
	{
		super();
	}

	public INTraceDefinitionTerm(TCTraceDefinitionTerm from) throws Exception
	{
		super(from);
	}
	
	public TraceIterator getIterator(Context ctxt)
	{
		TraceIteratorList list = new TraceIteratorList();

		for (INTraceDefinition term: this)
		{
			list.add(term.getIterator(ctxt));
		}

		return list.getAlternatveIterator();
	}
}
