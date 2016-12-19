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
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.ValueList;

public class INMultipleSeqBind extends INMultipleBind
{
	private static final long serialVersionUID = 1L;
	public final INExpression sequence;

	public INMultipleSeqBind(INPatternList plist, INExpression sequence)
	{
		super(plist);
		this.sequence = sequence;
	}

	@Override
	public String toString()
	{
		return plist + " in seq " + sequence;
	}

	@Override
	public ValueList getBindValues(Context ctxt, boolean permuted)
	{
		try
		{
			return sequence.eval(ctxt).seqValue(ctxt);
		}
		catch (ValueException e)
		{
			abort(e);
			return null;
		}
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		return sequence.getValues(ctxt);
	}

	@Override
	public TCNameList getOldNames()
	{
		return sequence.getOldNames();
	}
}
