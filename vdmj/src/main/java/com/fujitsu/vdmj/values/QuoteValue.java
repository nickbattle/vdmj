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

package com.fujitsu.vdmj.values;

import java.util.FormattableFlags;
import java.util.Formatter;

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public class QuoteValue extends Value
{
	private static final long serialVersionUID = 1L;
	public final String value;

	public QuoteValue(String value)
	{
		this.value = value;
	}

	@Override
	public String quoteValue(Context ctxt)
	{
		return value;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

    		if (val instanceof QuoteValue)
    		{
    			QuoteValue ov = (QuoteValue)val;
    			return ov.value.equals(value);
    		}
		}

		return false;
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision)
	{
		String s = toString();		// With '<...>' quotes, by default

		if ((flags & FormattableFlags.ALTERNATE) > 0)
		{
			s = value;
			flags = flags & ~FormattableFlags.ALTERNATE;
		}

		formatTo(s, formatter, flags, width, precision);
	}

	@Override
	public String toString()
	{
		return "<" + value + ">";
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
		if (to instanceof TCQuoteType)
		{
			TCQuoteType qto = (TCQuoteType)to;

			if (!qto.value.equals(value))
			{
				abort(4074, "Cannot convert " + this + " to " + to, ctxt);
			}

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
		return new QuoteValue(value);
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseQuoteValue(this, arg);
	}
}
