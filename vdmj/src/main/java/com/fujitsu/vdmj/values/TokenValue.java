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

package com.fujitsu.vdmj.values;

import java.util.FormattableFlags;
import java.util.Formatter;

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public class TokenValue extends Value
{
	private static final long serialVersionUID = 1L;
	private final Value value;

	public TokenValue(Value exp)
	{
		this.value = exp.deref();
	}

	@Override
	public String toString()
	{
		return "mk_token(" + value + ")";
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision)
	{
		String s = toString();		// With 'mk_token(...)', by default

		if ((flags & FormattableFlags.ALTERNATE) > 0)
		{
			s = value.toString();
			flags = flags & ~FormattableFlags.ALTERNATE;
		}

		formatTo(s, formatter, flags, width, precision);
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

    		if (val instanceof TokenValue)
    		{
    			TokenValue tok = (TokenValue)val;
    			return tok.value.equals(value);
    		}
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return value.hashCode();
	}

	@Override
	public String kind()
	{
		return toString();
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to instanceof TCTokenType)
		{
			return this;
		}
		else
		{
			return super.convertValueTo(to, ctxt, done);
		}
	}

	@Override
	public Object clone()
	{
		return new TokenValue(value);
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTokenValue(this, arg);
	}
}
