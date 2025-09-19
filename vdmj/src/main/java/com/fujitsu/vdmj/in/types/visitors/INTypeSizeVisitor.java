/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj.in.types.visitors;

import java.math.BigInteger;

import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCInMapType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUndefinedType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.tc.types.TCVoidReturnType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.values.ParameterValue;
import com.fujitsu.vdmj.values.Value;

public class INTypeSizeVisitor extends TCTypeVisitor<Long, Context>
{
	/**
	 * We have to collect the nodes that have already been visited since types can be recursive,
	 * and the visitor will otherwise blow the stack. Note that this means you need a new visitor
	 * instance for every use (or only re-use with care!). This is tested and modified in the
	 * NamedType and RecordType entries.
	 */
	protected TCTypeSet active = new TCTypeSet();

	@Override
	public Long caseType(TCType type, Context ctxt)
	{
		throw new RuntimeException("Missing INTypeSizeVisitor case for " + type);
	}

	@Override
	public Long caseBooleanType(TCBooleanType type, Context ctxt)
	{
		return 2L;
	}

	@Override
	public Long caseBracketType(TCBracketType type, Context ctxt)
	{
		return type.type.apply(this, ctxt);
	}

	@Override
	public Long caseMapType(TCMapType type, Context ctxt)
	{
		long f = type.from.apply(this, ctxt);
		long t = type.to.apply(this, ctxt);
		BigInteger r = BigInteger.ONE;	// +1 for the empty map
		
		for (int z=1; z<=f; z++)
		{
			r = r.add(combs(f, z).multiply(pow(t, z)));
		}
		
		return r.longValueExact();
	}

	@Override
	public Long caseInMapType(TCInMapType type, Context ctxt)
	{
		long f = type.from.apply(this, ctxt);
		long t = type.to.apply(this, ctxt);
		BigInteger r = BigInteger.ONE;	// +1 for the empty map
		
		for (int z=1; z<=f && z<=t; z++)
		{
			r = r.add(combs(f, z).multiply(perms(t, z)));
		}
		
		return r.longValueExact();
	}
	
	@Override
	public Long caseNamedType(TCNamedType type, Context ctxt)
	{
		if (active.contains(type))
		{
			infiniteType(type, ctxt);
		}

		active.add(type);
		Long size = type.type.apply(this, ctxt);
		active.remove(type);
		return size;
	}

	@Override
	public Long caseOptionalType(TCOptionalType type, Context ctxt)
	{
		return type.type.apply(this, ctxt) + 1;		// + 'nil'
	}

	@Override
	public Long caseParameterType(TCParameterType type, Context ctxt)
	{
		Value t = ctxt.lookup(type.name);

		if (t == null)
		{
			ExceptionHandler.abort(type.location, 4008, "No such type parameter @" + type.name + " in scope", ctxt);
		}
		else if (t instanceof ParameterValue)
		{
			ParameterValue tv = (ParameterValue)t;
			return tv.type.apply(this, ctxt);
		}
		
		ExceptionHandler.abort(type.location, 4009, "Type parameter/local variable name clash, @" + type.name, ctxt);
		return null;
	}

	@Override
	public Long caseProductType(TCProductType type, Context ctxt)
	{
		return ofTypeList(type.types, ctxt);
	}

	@Override
	public Long caseQuoteType(TCQuoteType type, Context ctxt)
	{
		return 1L;
	}

	@Override
	public Long caseRecordType(TCRecordType type, Context ctxt)
	{
		if (active.contains(type))
		{
			infiniteType(type, ctxt);
		}

		active.add(type);
		TCTypeList fieldtypes = new TCTypeList();

		for (TCField f: type.fields)
		{
			fieldtypes.add(f.type);
		}

		long size = ofTypeList(fieldtypes, ctxt);
		active.remove(type);
		return size;
	}

	@Override
	public Long caseSetType(TCSetType type, Context ctxt)
	{
		long n = type.setof.apply(this, ctxt);

		if (type instanceof TCSet1Type)
		{
			n = n - 1;
		}

		return pow(2, n).longValueExact();
	}

	@Override
	public Long caseUnionType(TCUnionType type, Context ctxt)
	{
		long n = 0;

		for (TCType member: type.types)
		{
			n = n + member.apply(this, ctxt);
		}

		return n;
	}
	
	/**
	 * The remaining types are "infinite" and all produce an error.
	 */
	
	@Override
	public Long caseCharacterType(TCCharacterType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}
	
	@Override
	public Long caseClassType(TCClassType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseFunctionType(TCFunctionType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseIntegerType(TCIntegerType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseNaturalOneType(TCNaturalOneType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseNaturalType(TCNaturalType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseNumericType(TCNumericType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseOperationType(TCOperationType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseRationalType(TCRationalType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseRealType(TCRealType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseSeqType(TCSeqType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseTokenType(TCTokenType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseUndefinedType(TCUndefinedType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseUnknownType(TCUnknownType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseUnresolvedType(TCUnresolvedType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseVoidReturnType(TCVoidReturnType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public Long caseVoidType(TCVoidType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	/**
	 * Factorial, binary-coefficient, permutations and power methods for caseMapType. These
	 * are evaluated using BigIntegers and then converted to longValueExact by the caller.
	 */
	private BigInteger fac(long n)
	{
		if (n == 0)
		{
			return BigInteger.ONE;
		}
		else
		{
			BigInteger N = new BigInteger(Long.toString(n));
			return N.multiply(fac(n-1));
		}
	}

	private BigInteger combs(long n, long k)	// k <= n
	{
		return fac(n).divide(fac(k).multiply(fac(n - k)));
	}

	private BigInteger perms(long n, long k)	// k <= n
	{
		return fac(n).divide(fac(n - k));
	}

	private BigInteger pow(long n, long k)
	{
		BigInteger r = BigInteger.ONE;
		BigInteger N = new BigInteger(Long.toString(n));
		
		for (int i=0; i<k; i++)
		{
			r = r.multiply(N);
		}
		
		return r;
	}

	/**
	 * Throw a runtime exception wrapping a ValueException cause. We need to add some sort
	 * of exception mechanism to visitors? 
	 */
	private Long infiniteType(TCType type, Context ctxt)
	{
		throw new InternalException(4, "Cannot get bind values for type " + type);
	}
	
	private Long ofTypeList(TCTypeList types, Context ctxt)
	{
		long n = 1;
		
		for (TCType t: types)
		{
			n = n * t.apply(this, ctxt);
		}

		return n;
	}

	public static void main(String[] args)
	{
		long a = 6;
		long b = 3;
		INTypeSizeVisitor obj = new INTypeSizeVisitor();

		System.out.println("f(a, b) = " + obj.perms(a, b));
	}
}
