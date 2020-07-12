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

package com.fujitsu.vdmj.in.patterns;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.visitors.INBindVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.ValueList;

public class INSeqBind extends INBind
{
	private static final long serialVersionUID = 1L;
	public final INExpression sequence;

	public INSeqBind(INPattern pattern, INExpression sequence)
	{
		super(pattern.location, pattern);
		this.sequence = sequence;
	}

	@Override
	public INMultipleBindList getMultipleBindList()
	{
		INPatternList plist = new INPatternList();
		plist.add(pattern);
		INMultipleBindList mblist = new INMultipleBindList();
		mblist.add(new INMultipleSeqBind(plist, sequence));
		return mblist;
	}

	@Override
	public String toString()
	{
		return pattern + " in seq " + sequence;
	}

	@Override
	public ValueList getBindValues(Context ctxt, boolean permuted) throws ValueException
	{
		return sequence.eval(ctxt).seqValue(ctxt);
	}

	@Override
	public TCNameList getOldNames()
	{
		return sequence.getOldNames();
	}

	@Override
	public <R, S> R apply(INBindVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSeqBind(this, arg);
	}
}
