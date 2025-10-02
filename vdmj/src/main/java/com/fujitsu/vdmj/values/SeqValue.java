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

import com.fujitsu.vdmj.ast.lex.LexStringToken;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.util.Utils;
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

	/**
	 * This produces a raw (Java) String of the contents, assuming it is a seq of char.
	 * Note that it therefore contains no quoted characters, like "\n" or "\"", and the
	 * result can be the empty string. If it's not a seq of char, the result is the
	 * standard string for the ValueList, which is [...].
	 */
	@Override
	public String stringValue(Context ctxt)
	{
		StringBuilder sb = new StringBuilder();

		for (Value v: values)
		{
			v = v.deref();

			if (!(v instanceof CharacterValue))
			{
				return values.toString();	// [...] format
			}

			CharacterValue ch = (CharacterValue)v;
			sb.append(ch.unicode);			// Note: unquoted
		}

		return sb.toString();
	}

	/**
	 * This formatter uses the ALTERNATE flag to give a more "raw" output. So
	 * an empty sequence is a blank string, a "value" in quotes has the quotes
	 * removed, and a [seq, of, values] has the outer brackets removed.
	 * 
	 * Use with %#10s, for an alternate format within a 10 char width field.
	 */
	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision)
	{
		String s = stringValue(null);	// Either [1,2,3], or Hello, with "\n" etc converted

		if (!s.startsWith("["))
		{
			s = "\"" + s + "\"";			// Add "quotes" back for a string
		}

		if ((flags & FormattableFlags.ALTERNATE) > 0)
		{
			if (values.isEmpty())
			{
				s = "";
			}
			else
			{
				s = s.substring(1, s.length()-1);	// Without "quotes" or "[ ... ]"
			}

			flags = flags & ~FormattableFlags.ALTERNATE;
		}

		formatTo(s, formatter, flags, width, precision);
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
		int i = (int)arg.nat1Value(ctxt).intValue();

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

	/**
	 * If the sequence is empty, it is shown as "[]", else we start to generate a sequence
	 * as if it is a "String", quoting each character. If anything non-character appears in
	 * the list, a "[...]" format string is produced.
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		if (values.isEmpty())
		{
			sb.append("[]");
		}
		else
		{
			sb.append("\"");

    		for (Value v: values)
    		{
    			v = v.deref();

    			if (!(v instanceof CharacterValue))
    			{
    				return Utils.listToString("[", values, ", ", "]");
    			}

    			CharacterValue ch = (CharacterValue)v;
				sb.append(Utils.quote(ch.unicode));
    		}

    		sb.append("\"");
		}

		return sb.toString();
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
