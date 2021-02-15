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

import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.traces.TraceIterator;

/**
 * A class representing a let-definition trace binding.
 */
public class INTraceLetDefBinding extends INTraceDefinition
{
    private static final long serialVersionUID = 1L;
	public final INDefinitionList localDefs;
	public final INTraceDefinition body;

	public INTraceLetDefBinding(
		LexLocation location, INDefinitionList localDefs, INTraceDefinition body)
	{
		super(location);
		this.localDefs = localDefs;
		this.body = body;
	}

	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder("let ");

		for (INDefinition d: localDefs)
		{
			result.append(d.toString());
			result.append(" ");
		}

		result.append("in ");
		result.append(body);

		return result.toString();
	}

	@Override
	public TraceIterator getIterator(Context ctxt)
	{
		Context evalContext = new Context(location, "TRACE", ctxt);

		for (INDefinition d: localDefs)
		{
			evalContext.putList(d.getNamedValues(evalContext));
		}

		TraceIterator iterator = body.getIterator(evalContext);
		iterator.setVariables(new INTraceVariableList(evalContext, localDefs));
		return iterator;
	}
}
