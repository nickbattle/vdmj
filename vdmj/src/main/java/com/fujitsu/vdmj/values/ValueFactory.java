/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * Create Values from the arguments passed, which is useful from native Java implementations.
 */
public class ValueFactory
{
	public static BooleanValue mkBool(boolean b)
	{
		return new BooleanValue(b);
	}
	
	public static CharacterValue mkChar(char c)
	{
		return new CharacterValue(c);
	}
	
	public static IntegerValue mkInt(BigInteger i)
	{
		return new IntegerValue(i);
	}
	
	public static NaturalValue mkNat(BigInteger n) throws Exception
	{
		return new NaturalValue(n);
	}
	
	public static NaturalOneValue mkNat1(BigInteger n) throws Exception
	{
		return new NaturalOneValue(n);
	}
	
	public static RationalValue mkRat(BigInteger p, BigInteger q) throws Exception
	{
		return new RationalValue(new BigDecimal(p).divide(new BigDecimal(q)));
	}

	public static RationalValue mkRat(BigDecimal d) throws Exception
	{
		return new RationalValue(d);
	}

	public static RealValue mkReal(BigDecimal d) throws Exception
	{
		return new RealValue(d);
	}

	public static NilValue mkNil()
	{
		return new NilValue();
	}
	
	public static QuoteValue mkQuote(String q)
	{
		return new QuoteValue(q);
	}
	
	public static SeqValue mkSeq(Value ...args)
	{
		return new SeqValue(new ValueList(args));
	}
	
	public static SeqValue mkSeq(String arg)
	{
		return new SeqValue(arg);
	}
	
	public static SetValue mkSet(Value ...args)
	{
		return new SetValue(new ValueSet(args));
	}
	
	public static TupleValue mkTuple(Value ...args)
	{
		return new TupleValue(new ValueList(args));
	}
	
	public static TokenValue mkToken(Value arg)
	{
		return new TokenValue(arg);
	}
	
	public static RecordValue mkRecord(String module, String name, Value ...args) throws ValueException
	{
		TCType type = getType(module, name);
		
		if (type instanceof TCRecordType)
		{
    		TCRecordType r = (TCRecordType)type;
    		ValueList l = new ValueList();
    		
    		for (int a=0; a<args.length; a++)
    		{
    			l.add(args[a]);
    		}
    		
    		return new RecordValue(r, l, Interpreter.getInstance().getInitialContext());
		}
		else
		{
			throw new ValueException(69, "Definition " + module + "`" + name +
				" is " + type.getClass().getSimpleName() + " not TCRecordType", Context.javaContext());
		}
	}

	public static InvariantValue mkInvariant(String module, String name, Value x) throws ValueException
	{
		TCType type = getType(module, name);
		
		if (type instanceof TCNamedType)
		{
			TCNamedType r = (TCNamedType)type;
			return new InvariantValue(r, x, Interpreter.getInstance().getInitialContext());
		}
		else
		{
			throw new ValueException(69, "Definition " + module + "`" + name +
				" is " + type.getClass().getSimpleName() + " not TCNamedType", Context.javaContext());
		}
	}
	
	private static TCType getType(String module, String name) throws ValueException
	{
		Interpreter i = Interpreter.getInstance();
		TCNameToken tcname = new TCNameToken(LexLocation.ANY, module, name);
		TCDefinition def = i.getGlobalEnvironment().findType(tcname, i.getDefaultName());
		
		if (def == null)
		{
			throw new ValueException(70, "Definition " + tcname.getExplicit(true) + " not found", Context.javaContext());
		}
		
		return def.getType();
	}
}
