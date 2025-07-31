/*******************************************************************************
 *
 *	Copyright (c) 2025 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.po.types.visitors;

import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCInMapType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
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
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CharacterValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.MapValue;
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
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;

/**
 * A visitor to analyse a type and return a Value that is a valid default
 * value of that type. For example, a "nat1" type might have a value of "0"
 * as a IntegerValue.
 */
public class DefaultValueCreator extends TCTypeVisitor<Value, Object>
{
	public DefaultValueCreator()
	{
		// noting to do
	}

	@Override
	public Value caseType(TCType node, Object arg)
	{
		throw new UnsupportedOperationException("Cannot calculate default for " + node);
	}
	
	@Override
	public Value caseBooleanType(TCBooleanType node, Object arg)
	{
		return new BooleanValue(true);
	}

	@Override
	public Value caseBracketType(TCBracketType node, Object arg)
	{
		return node.type.apply(this, null);
	}

	@Override
	public Value caseCharacterType(TCCharacterType node, Object arg)
	{
		return new CharacterValue('?');
	}

	@Override
	public Value caseInMapType(TCInMapType node, Object arg)
	{
		return new MapValue();
	}

	@Override
	public Value caseIntegerType(TCIntegerType node, Object arg)
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
		return node.type.apply(this, null);
	}

	@Override
	public Value caseNaturalOneType(TCNaturalOneType node, Object arg)
	{
		try
		{
			return new NaturalOneValue(1);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Override
	public Value caseNaturalType(TCNaturalType node, Object arg)
	{
		try
		{
			return new NaturalValue(0);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Override
	public Value caseOptionalType(TCOptionalType node, Object arg)
	{
		return new NilValue();
	}

	@Override
	public Value caseProductType(TCProductType node, Object arg)
	{
		ValueList list = new ValueList();

		for (TCType type: node.types)
		{
			list.add(type.apply(this, null));
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
		return new RationalValue(0);
	}

	@Override
	public Value caseRealType(TCRealType node, Object arg)
	{
		return new RealValue(0);
	}

	@Override
	public Value caseRecordType(TCRecordType node, Object arg)
	{
		NameValuePairList values = new NameValuePairList();

		for (TCField field: node.fields)
		{
			values.add(field.tagname, field.type.apply(this, null));
		}

		return new RecordValue(node, values);
	}

	@Override
	public Value caseSeq1Type(TCSeq1Type node, Object arg)
	{
		ValueList values = new ValueList();
		values.add(node.seqof.apply(this, null));
		return new SeqValue(values);
	}

	@Override
	public Value caseSeqType(TCSeqType node, Object arg)
	{
		return new SeqValue();
	}

	@Override
	public Value caseSet1Type(TCSet1Type node, Object arg)
	{
		ValueSet values = new ValueSet();
		values.add(node.setof.apply(this, null));
		return new SetValue(values);
	}

	@Override
	public Value caseSetType(TCSetType node, Object arg)
	{
		return new SetValue();
	}

	@Override
	public Value caseTokenType(TCTokenType node, Object arg)
	{
		return new TokenValue(new NilValue());
	}

	@Override
	public Value caseUnionType(TCUnionType node, Object arg)
	{
		return node.types.first().apply(this, null);
	}
}