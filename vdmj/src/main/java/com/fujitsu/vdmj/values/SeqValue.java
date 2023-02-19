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

import com.fujitsu.vdmj.ast.lex.LexStringToken;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public class SeqValue extends Value
{
	private static final long serialVersionUID = 1L;
	public final ValueList values;

	public SeqValue()
	{
		values = new ValueList();
	}

	public SeqValue(ValueList values)
	{
		this.values = values;
	}

	public SeqValue(String s)
	{
		this.values = new ValueList();
		int len = s.length();

		for (int i=0; i<len; i++)
		{
			this.values.add(new CharacterValue(s.charAt(i)));
		}
	}

	public SeqValue(LexStringToken string)
	{
		this(string.value);
	}

	@Override
	public ValueList seqValue(Context ctxt)
	{
		return values;
	}

	@Override
	public String stringValue(Context ctxt)
	{
		String s = values.toString();

		if (s.charAt(0) == '"')
		{
			return s.substring(1, s.length()-1);
		}

		return s;
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision)
	{
		String s = values.toString();

		if ((flags & FormattableFlags.ALTERNATE) == 0 && s.charAt(0) == '"')
		{
			s = s.substring(1, s.length()-1);	// Without "quotes"
		}

		formatTo(s, formatter, 0, width, precision);
	}

	@Override
	public UpdatableValue getUpdatable(ValueListenerList listeners)
	{
		ValueList nseq = new ValueList();

		for (Value k: values)
		{
			Value v = k.getUpdatable(listeners);
			nseq.add(v);
		}

		return UpdatableValue.factory(new SeqValue(nseq), listeners);
	}

	@Override
	public Value getConstant()
	{
		return new SeqValue(values.getConstant());
	}

	public Value get(Value arg, Context ctxt) throws ValueException
	{
		int i = (int)arg.nat1Value(ctxt);

		if (i < 1 || i > values.size())
		{
			abort(4083, "Sequence index out of range: " + arg, ctxt);
		}

		return values.get(i-1);		// NB 1st = 1. Throws IndexOutOfBounds
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

    		if (val instanceof SeqValue)
    		{
    			SeqValue ot = (SeqValue)val;
    			return values.equals(ot.values);
    		}
		}

		return false;
	}

	@Override
	public String toString()
	{
		return values.toString();
	}

	@Override
	public int hashCode()
	{
		return values.hashCode();
	}

	@Override
	public String kind()
	{
		return "seq";
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		// We can't use the isSeq method as it plucks out one sequence
		// value from a union. We need to try all union members. So we
		// only test for pure SeqTypes.

		if (to instanceof TCSeqType)
		{
			if (to instanceof TCSeq1Type && values.isEmpty())
			{
				abort(4084, "Cannot convert empty sequence to seq1", ctxt);
			}

			TCSeqType seqto = (TCSeqType)to;
			ValueList nl = new ValueList();

			for (Value v: values)
			{
				nl.add(v.convertValueTo(seqto.seqof, ctxt));
			}

			return new SeqValue(nl);
		}
		else
		{
			return super.convertValueTo(to, ctxt, done);
		}
	}

	@Override
	public Object clone()
	{
		return new SeqValue((ValueList)values.clone());
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSeqValue(this, arg);
	}
}
