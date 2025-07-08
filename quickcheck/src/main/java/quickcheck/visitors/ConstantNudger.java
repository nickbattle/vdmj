/*******************************************************************************
 *
 *	Copyright (c) 2024 Nick Battle.
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

package quickcheck.visitors;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map.Entry;

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CharacterValue;
import com.fujitsu.vdmj.values.FieldValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.InvariantValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NaturalOneValue;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.RationalValue;
import com.fujitsu.vdmj.values.RealValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueMap;
import com.fujitsu.vdmj.values.ValueSet;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

/**
 * A class to nudge a constant Value into another very similar value.
 */
public class ConstantNudger extends ValueVisitor<Value, Integer>
{
	private final Context ctxt;
	
	public ConstantNudger()
	{
		ctxt = Interpreter.getInstance().getInitialContext();
	}
	
	@Override
	public Value caseBooleanValue(BooleanValue node, Integer arg)
	{
		return new BooleanValue(!node.value);
	}
	
	@Override
	public Value caseCharacterValue(CharacterValue node, Integer arg)
	{
		return new CharacterValue((char)(node.unicode + arg));
	}
	
	@Override
	public Value caseMapValue(MapValue node, Integer arg)
	{
		ValueMap nudged = new ValueMap();
		
		for (Entry<Value, Value> entry: node.values.entrySet())
		{
			Value key = entry.getKey().apply(this, arg);
			Value val = entry.getValue().apply(this, arg);
			
			nudged.put(key, val);
		}
		
		return new MapValue(nudged);
	}
	
	@Override
	public Value caseRecordValue(RecordValue node, Integer arg)
	{
		ValueList nudged = new ValueList();
		
		for (FieldValue fieldValue: node.fieldmap)
		{
			nudged.add(fieldValue.value.apply(this, arg));
		}
		
		try
		{
			return new RecordValue(node.type, nudged, ctxt);
		}
		catch (ValueException e)
		{
			return node;
		}
	}
	
	@Override
	public Value caseInvariantValue(InvariantValue node, Integer arg)
	{
		Value nudged = node.getValue().apply(this, arg);
		
		try
		{
			return new InvariantValue(node.type, nudged, ctxt);
		}
		catch (ValueException e)
		{
			return node;
		}
	}
	
	@Override
	public Value caseSeqValue(SeqValue node, Integer arg)
	{
		ValueList nudged = new ValueList();
		
		for (Value v: node.values)
		{
			nudged.add(v.apply(this, arg));
		}
		
		return new SeqValue(nudged);
	}
	
	@Override
	public Value caseSetValue(SetValue node, Integer arg)
	{
		ValueSet nudged = new ValueSet();
		
		for (Value v: node.values)
		{
			nudged.add(v.apply(this, arg));
		}
		
		return new SetValue(nudged);
	}

	@Override
	public Value caseTupleValue(TupleValue node, Integer arg)
	{
		ValueList nudged = new ValueList();
		
		for (Value v: node.values)
		{
			nudged.add(v.apply(this, arg));
		}
		
		return new TupleValue(nudged);
	}
	
	@Override
	public Value caseNaturalOneValue(NaturalOneValue node, Integer arg)
	{
		try
		{
			return new NaturalOneValue(node.nat1Value(ctxt).add(BigInteger.valueOf(arg)));
		}
		catch (Exception e)
		{
			return node;
		}
	}
	
	@Override
	public Value caseNaturalValue(NaturalValue node, Integer arg)
	{
		try
		{
			return new NaturalValue(node.natValue(ctxt).add(BigInteger.valueOf(arg)));
		}
		catch (Exception e)
		{
			return node;
		}
	}
	
	@Override
	public Value caseIntegerValue(IntegerValue node, Integer arg)
	{
		return new IntegerValue(node.intValue(ctxt).add(BigInteger.valueOf(arg)));
	}
	
	@Override
	public Value caseRealValue(RealValue node, Integer arg)
	{
		try
		{
			return new RealValue(node.realValue(ctxt).add(BigDecimal.valueOf(arg)));
		}
		catch (Exception e)
		{
			return node;
		}
	}
	
	@Override
	public Value caseRationalValue(RationalValue node, Integer arg)
	{
		try
		{
			return new RationalValue(node.ratValue(ctxt).add(BigDecimal.valueOf(arg)));
		}
		catch (Exception e)
		{
			return node;
		}
	}
	
	@Override
	public Value caseValue(Value node, Integer arg)
	{
		return node;
	}
}
