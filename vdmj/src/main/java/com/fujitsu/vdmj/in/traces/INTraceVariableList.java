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

import java.util.LinkedList;

import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.traces.CallSequence;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;

public class INTraceVariableList extends LinkedList<INTraceVariable>
{
	private static final long serialVersionUID = 1L;

	public INTraceVariableList()
	{
		super();
	}

	public INTraceVariableList(Context ctxt, INDefinitionList defs)
	{
		for (TCNameToken key: ctxt.keySet())
		{
			Value value = ctxt.get(key);
			INDefinition d = defs.findName(key);
			boolean clone = false;
			
			if (value.isType(ObjectValue.class))
			{
				ObjectValue obj = (ObjectValue)value.deref();
				ObjectValue self = ctxt.getSelf();
				
				// We have to clone new objects that were created within the trace,
				// while using other (local instance variable) objects unchanged. 
				clone = (self != null && obj.objectReference > self.objectReference);
			}

			add(new INTraceVariable(key, value, d.getType(), clone));
		}
	}

	public CallSequence getVariables()
	{
		CallSequence seq = new CallSequence();

		for (INTraceVariable var: this)
		{
			seq.add(new INTraceVariableStatement(var));
		}

		return seq;
	}
}
