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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.traces.TraceIterator;
import com.fujitsu.vdmj.traces.TraceIteratorList;

/**
 * A class representing a core trace bracketed expression.
 */
public class INTraceBracketedExpression extends INTraceCoreDefinition
{
    private static final long serialVersionUID = 1L;
	public final INTraceDefinitionTermList terms;

	public INTraceBracketedExpression(LexLocation location, INTraceDefinitionTermList terms)
	{
		super(location);
		this.terms = terms;
	}

	@Override
	public String toString()
	{
		return terms.toString();
	}

	@Override
	public TraceIterator getIterator(Context ctxt)
	{
		TraceIteratorList list = new TraceIteratorList();

		for (INTraceDefinitionTerm term: terms)
		{
			list.add(term.getIterator(ctxt));
		}

		return list.getSequenceIterator();
	}
}
