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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.patterns.visitors.INPatternVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NilValue;
import com.fujitsu.vdmj.values.Value;

public class INNilPattern extends INPattern
{
	private static final long serialVersionUID = 1L;

	public INNilPattern(LexLocation location)
	{
		super(location);
	}

	@Override
	public String toString()
	{
		return "nil";
	}

	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt)
		throws PatternMatchException
	{
		List<NameValuePairList> result = new Vector<NameValuePairList>();

		if (!(expval.deref() instanceof NilValue))
		{
			patternFail(4106, "Nil pattern match failed");
		}

		result.add(new NameValuePairList());
		return result;
	}

	@Override
	protected TCType getPossibleType()
	{
		return new TCOptionalType(location, new TCUnknownType(location));
	}

	@Override
	public <R, S> R apply(INPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNilPattern(this, arg);
	}
}
