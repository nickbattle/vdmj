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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.traces.INTraceDefinitionTerm;
import com.fujitsu.vdmj.in.traces.INTraceDefinitionTermList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.traces.TraceIterator;
import com.fujitsu.vdmj.traces.TraceIteratorList;

public class INNamedTraceDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final INTraceDefinitionTermList terms;

	public INNamedTraceDefinition(INAnnotationList annotations, LexLocation location, TCNameToken pathname,
		INTraceDefinitionTermList terms, INClassDefinition classdef)
	{
		super(location,	null, pathname);
		this.annotations = annotations;
		this.terms = terms;
		this.classDefinition = classdef;
	}

	@Override
	public boolean isOperation()
	{
		return true;
	}

	@Override
	public TCType getType()
	{
		return new TCOperationType(location);		// () ==> ()
	}

	@Override
	public String toString()
	{
		return name + " = " + terms.toString();
	}

	public TraceIterator getIterator(Context ctxt) throws Exception
	{
		TraceIteratorList iterators = new TraceIteratorList();

		for (INTraceDefinitionTerm term: terms)
		{
			iterators.add(term.getIterator(ctxt));
		}

		if (iterators.isEmpty())
		{
			throw new Exception("Trace expansion generated no tests");
		}

		return iterators.getSequenceIterator();
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNamedTraceDefinition(this, arg);
	}
}
