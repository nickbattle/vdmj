/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package com.fujitsu.vdmj.tc.types.visitors;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fujitsu.vdmj.tc.types.TCBasicType;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCInMapType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUndefinedType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.tc.types.TCVoidReturnType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CharacterValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NaturalOneValue;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.NilValue;
import com.fujitsu.vdmj.values.QuoteValue;
import com.fujitsu.vdmj.values.RationalValue;
import com.fujitsu.vdmj.values.RealValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.TokenValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.UndefinedValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;
import com.fujitsu.vdmj.values.VoidReturnValue;
import com.fujitsu.vdmj.values.VoidValue;

public class TCAnyValueGenerator extends TCTypeVisitor<Value, Object>
{
	@Override
	public Value caseType(TCType node, Object arg)
	{
		throw new RuntimeException("Missing TCAnyValueGenerator method");
	}
	
	@Override
	public Value caseBasicType(TCBasicType node, Object arg)
	{
		return caseType(node, arg);
	}

	@Override
	public Value caseBooleanType(TCBooleanType node, Object arg)
	{
		return new BooleanValue(true);
	}

	@Override
	public Value caseBracketType(TCBracketType node, Object arg)
	{
		return node.type.apply(this, arg);
	}

	@Override
	public Value caseCharacterType(TCCharacterType node, Object arg)
	{
		return new CharacterValue('a');
	}

	@Override
	public Value caseClassType(TCClassType node, Object arg)
	{
		return null;	// Can't do this?
	}

	@Override
	public Value caseFunctionType(TCFunctionType node, Object arg)
	{
		return null;	// Can't do this?
	}

	@Override
	public Value caseInMapType(TCInMapType node, Object arg)
	{
		return new MapValue();
	}

	@Override
	public Value caseIntegerType(TCIntegerType node, Object arg)
	{
		return caseNumericType(node, arg);
	}

	@Override
	public Value caseInvariantType(TCInvariantType node, Object arg)
	{
		return new IntegerValue(0);
	}

	@Override
	public Value caseMapType(TCMapType node, Object arg)
	{
		return new MapValue();
	}

	@Override
	public Value caseNamedType(TCNamedType node, Object arg)
	{
		return node.type.apply(this, arg);
	}

	@Override
	public Value caseNaturalOneType(TCNaturalOneType node, Object arg)
	{
		try
		{
			return new NaturalOneValue(BigInteger.ONE);
		}
		catch (Exception e)
		{
			return null;	// can't happen
		}
	}

	@Override
	public Value caseNaturalType(TCNaturalType node, Object arg)
	{
		try
		{
			return new NaturalValue(BigInteger.ONE);
		}
		catch (Exception e)
		{
			return null;	// can't happen
		}
	}

	@Override
	public Value caseOperationType(TCOperationType node, Object arg)
	{
		return null;	// Can't do this one?
	}

	@Override
	public Value caseOptionalType(TCOptionalType node, Object arg)
	{
		return new NilValue();
	}

	@Override
	public Value caseParameterType(TCParameterType node, Object arg)
	{
		return null;	// Can't do this one?
	}

	@Override
	public Value caseProductType(TCProductType node, Object arg)
	{
		ValueList list = new ValueList();
		
		for (TCType type: node.types)
		{
			list.add(type.apply(this, arg));
		}
		
		return new TupleValue(list);
	}

	@Override
	public Value caseQuoteType(TCQuoteType node, Object arg)
	{
		return new QuoteValue(node.value);
	}

	@Override
	public Value caseRationalType(TCRationalType node, Object arg)
	{
		try
		{
			return new RationalValue(BigDecimal.ZERO);
		}
		catch (Exception e)
		{
			return null;	// Can't happen
		}
	}

	@Override
	public Value caseRealType(TCRealType node, Object arg)
	{
		try
		{
			return new RealValue(BigDecimal.ZERO);
		}
		catch (Exception e)
		{
			return null;	// Can't happen
		}
	}

	@Override
	public Value caseRecordType(TCRecordType node, Object arg)
	{
		NameValuePairList list = new NameValuePairList();
		
		for (TCField field: node.fields)
		{
			list.add(new NameValuePair(field.tagname, field.type.apply(this, arg)));
		}
		
		return new RecordValue(node, list);
	}

	@Override
	public Value caseSeq1Type(TCSeq1Type node, Object arg)
	{
		ValueList list = new ValueList(node.seqof.apply(this, arg));
		return new SeqValue(list);
	}

	@Override
	public Value caseSeqType(TCSeqType node, Object arg)
	{
		return new SeqValue();
	}

	@Override
	public Value caseSet1Type(TCSet1Type node, Object arg)
	{
		ValueSet list = new ValueSet(node.setof.apply(this, arg));
		return new SetValue(list);
	}

	@Override
	public Value caseSetType(TCSetType node, Object arg)
	{
		return new SetValue();
	}

	@Override
	public Value caseTokenType(TCTokenType node, Object arg)
	{
		return new TokenValue(node.argtypes.first().apply(this, arg));
	}

	@Override
	public Value caseUndefinedType(TCUndefinedType node, Object arg)
	{
		return new UndefinedValue();
	}

	@Override
	public Value caseUnionType(TCUnionType node, Object arg)
	{
		return node.types.first().apply(this, arg);		// Just pick one
	}

	@Override
	public Value caseUnknownType(TCUnknownType node, Object arg)
	{
		return new UndefinedValue();
	}

	@Override
	public Value caseUnresolvedType(TCUnresolvedType node, Object arg)
	{
		return new UndefinedValue();	// Should never happen
	}

	@Override
	public Value caseVoidReturnType(TCVoidReturnType node, Object arg)
	{
		return new VoidReturnValue();
	}

	@Override
	public Value caseVoidType(TCVoidType node, Object arg)
	{
		return new VoidValue();
	}
}
