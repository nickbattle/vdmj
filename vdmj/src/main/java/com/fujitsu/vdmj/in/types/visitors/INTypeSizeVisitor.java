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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
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
import com.fujitsu.vdmj.tc.types.TCUndefinedType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.tc.types.TCVoidReturnType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.values.ParameterValue;
import com.fujitsu.vdmj.values.Value;

public class INTypeSizeVisitor extends TCTypeVisitor<BigInteger, Context>
{
	@Override
	public BigInteger caseType(TCType type, Context ctxt)
	{
		throw new RuntimeException("Missing INTypeSizeVisitor case for " + type);
	}

	@Override
	public BigInteger caseBooleanType(TCBooleanType type, Context ctxt)
	{
		return new BigInteger("2");
	}

	@Override
	public BigInteger caseBracketType(TCBracketType type, Context ctxt)
	{
		return type.type.apply(this, ctxt);
	}

	@Override
	public BigInteger caseMapType(TCMapType type, Context ctxt)
	{
		BigInteger f = type.from.apply(this, ctxt);
		BigInteger t = type.to.apply(this, ctxt);
		BigInteger r = BigInteger.ONE;	// +1 for the empty map
		
		for (BigInteger z = BigInteger.ONE; z.compareTo(f) <= 0; z = z.add(BigInteger.ONE))
		{
			r = r.add(combs(f, z.multiply(pow(t, z))));
		}
		
		return r;
	}

	@Override
	public BigInteger caseInMapType(TCInMapType type, Context ctxt)
	{
		BigInteger f = type.from.apply(this, ctxt);
		BigInteger t = type.to.apply(this, ctxt);
		BigInteger r = BigInteger.ONE;	// +1 for the empty map
		
		for (BigInteger z = BigInteger.ONE; z.compareTo(f) <= 0 && z.compareTo(t) <= 0; z = z.add(BigInteger.ONE))
		{
			r = r.add(combs(f, z).multiply(perms(t, z)));
		}
		
		return r;
	}
	
	@Override
	public BigInteger caseNamedType(TCNamedType type, Context ctxt)
	{
		return type.type.apply(this, ctxt);
	}

	@Override
	public BigInteger caseOptionalType(TCOptionalType type, Context ctxt)
	{
		return type.type.apply(this, ctxt).add(BigInteger.ONE);		// + 'nil'
	}

	@Override
	public BigInteger caseParameterType(TCParameterType type, Context ctxt)
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
	public BigInteger caseProductType(TCProductType type, Context ctxt)
	{
		return ofTypeList(type.types, ctxt);
	}

	@Override
	public BigInteger caseQuoteType(TCQuoteType type, Context ctxt)
	{
		return BigInteger.ONE;
	}

	@Override
	public BigInteger caseRecordType(TCRecordType type, Context ctxt)
	{
		TCTypeList fieldtypes = new TCTypeList();

		for (TCField f: type.fields)
		{
			fieldtypes.add(f.type);
		}

		return ofTypeList(fieldtypes, ctxt);
	}

	@Override
	public BigInteger caseSetType(TCSetType type, Context ctxt)
	{
		BigInteger n = type.setof.apply(this, ctxt);

		if (type instanceof TCSet1Type)
		{
			n = n.subtract(BigInteger.ONE);
		}

		return pow(new BigInteger("2"), n);
	}

	@Override
	public BigInteger caseUnionType(TCUnionType type, Context ctxt)
	{
		BigInteger n = BigInteger.ZERO;

		for (TCType member: type.types)
		{
			n = n.add(member.apply(this, ctxt));
		}

		return n;
	}
	
	/**
	 * The remaining types are "infinite" and all produce an error.
	 */
	
	@Override
	public BigInteger caseCharacterType(TCCharacterType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}
	
	@Override
	public BigInteger caseClassType(TCClassType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseFunctionType(TCFunctionType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseIntegerType(TCIntegerType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseNaturalOneType(TCNaturalOneType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseNaturalType(TCNaturalType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseNumericType(TCNumericType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseOperationType(TCOperationType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseRationalType(TCRationalType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseRealType(TCRealType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseSeqType(TCSeqType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseTokenType(TCTokenType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseUndefinedType(TCUndefinedType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseUnknownType(TCUnknownType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseUnresolvedType(TCUnresolvedType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseVoidReturnType(TCVoidReturnType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public BigInteger caseVoidType(TCVoidType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	/**
	 * Factorial, binary-coefficient, permutations and power methods for caseMapType
	 */
	private BigInteger fac(BigInteger n)
	{
		if (n.equals(BigInteger.ZERO))
		{
			return BigInteger.ONE;
		}
		else
		{
			long nl = n.longValue();
			BigInteger r = BigInteger.ONE;
			
			for (long i = 2; i < nl; i++)
			{
				r = r.multiply(new BigInteger(Long.toString(i)));
			}
			
			return r;
		}
	}

	private BigInteger combs(BigInteger n, BigInteger k)	// k <= n
	{
		return fac(n).divide(fac(k).multiply(fac(n.subtract(k))));
	}

	private BigInteger perms(BigInteger n, BigInteger k)	// k <= n
	{
		return fac(n).divide(fac(n.subtract(k)));
	}

	private BigInteger pow(BigInteger n, BigInteger k)
	{
		return n.pow(k.intValueExact());
	}

	/**
	 * Throw a runtime exception wrapping a ValueException cause. We need to add some sort
	 * of exception mechanism to visitors? 
	 */
	private BigInteger infiniteType(TCType type, Context ctxt)
	{
		throw new InternalException(4, "Cannot get bind values for type " + type);
	}
	
	private BigInteger ofTypeList(TCTypeList types, Context ctxt)
	{
		BigInteger n = BigInteger.ONE;
		
		for (TCType t: types)
		{
			n = n.multiply(t.apply(this, ctxt));
		}

		return n;
	}
}
