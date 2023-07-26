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

package quickcheck.visitors;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
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
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.util.DuplicateKPermutor;
import com.fujitsu.vdmj.util.KCombinator;
import com.fujitsu.vdmj.util.KPermutor;
import com.fujitsu.vdmj.util.Selector;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CharacterValue;
import com.fujitsu.vdmj.values.FieldMap;
import com.fujitsu.vdmj.values.FieldValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.InvariantValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NaturalOneValue;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.NilValue;
import com.fujitsu.vdmj.values.QuoteValue;
import com.fujitsu.vdmj.values.RealValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.TokenValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueMap;
import com.fujitsu.vdmj.values.ValueSet;

public class InternalRangeCreator extends TCTypeVisitor<ValueSet, Integer>
{
	private final int numSetSize;	// {1, ..., N} for numerics
	private final Context ctxt;
	private final TCTypeSet done;
	
	public InternalRangeCreator(Context ctxt, int numSetSize)
	{
		this.ctxt = ctxt;
		this.numSetSize = numSetSize;
		this.done = new TCTypeSet();
	}

	@Override
	public ValueSet caseType(TCType type, Integer limit)
	{
		throw new RuntimeException("Missing InternalRangeCreator case for " + type);
	}
	
	@Override
	public ValueSet caseUnknownType(TCUnknownType node, Integer limit)
	{
		// Anything... ?
		return caseBooleanType(new TCBooleanType(LexLocation.ANY), limit);
	}

	@Override
	public ValueSet caseBooleanType(TCBooleanType type, Integer limit)
	{
		switch (limit)
		{
			case 0:		return new ValueSet();
			case 1:		return new ValueSet(new BooleanValue(false));
			default:	return new ValueSet(new BooleanValue(true), new BooleanValue(false));
		}
	}
	
	@Override
	public ValueSet caseCharacterType(TCCharacterType node, Integer limit)
	{
		switch (limit)
		{
			case 0:		return new ValueSet();
			case 1:		return new ValueSet(new CharacterValue('a'));
			default:	return new ValueSet(new CharacterValue('a'), new CharacterValue('b'));
		}
	}
	
	@Override
	public ValueSet caseTokenType(TCTokenType node, Integer limit)
	{
		switch (limit)
		{
			case 0:		return new ValueSet();
			case 1:		return new ValueSet(new TokenValue(new IntegerValue(1)));
			default:	return new ValueSet(new TokenValue(new IntegerValue(1)), new TokenValue(new IntegerValue(2)));
		}
	}
	
	@Override
	public ValueSet caseOptionalType(TCOptionalType node, Integer limit)
	{
		switch (limit)
		{
			case 0:		return new ValueSet();
			case 1:		return new ValueSet(new NilValue());
			default:
				ValueSet list = node.type.apply(this, limit - 1);
				list.add(new NilValue());
				return list;
		}
	}
	
	@Override
	public ValueSet caseBracketType(TCBracketType node, Integer limit)
	{
		return node.type.apply(this, limit);
	}

	@Override
	public ValueSet caseQuoteType(TCQuoteType node, Integer limit)
	{
		return new ValueSet(new QuoteValue(node.value));
	}
	
	@Override
	public ValueSet caseNaturalOneType(TCNaturalOneType node, Integer limit)
	{
		int to = numSetSize;

		if (limit < numSetSize)
		{
			to = limit;
		}

		ValueSet result = new ValueSet();
		
		for (long a = 1; a <= to; a++)
		{
			try
			{
				result.add(new NaturalOneValue(a));
			}
			catch (Exception e)
			{
				// Can't happen
			}
		}
		
		return result;
	}
	
	@Override
	public ValueSet caseNaturalType(TCNaturalType node, Integer limit)
	{
		int to = numSetSize - 1;
		
		if (limit < numSetSize - 1)
		{
			to = limit - 1;
		}

		ValueSet result = new ValueSet();
		
		for (long a = 0; a <= to; a++)
		{
			try
			{
				result.add(new NaturalValue(a));
			}
			catch (Exception e)
			{
				// Can't happen
			}
		}
		
		return result;
	}
	
	@Override
	public ValueSet caseIntegerType(TCIntegerType node, Integer limit)
	{
		int from = 0;
		int to = 0;
		
		if (limit < numSetSize * 2 + 1)
		{
			int half = limit / 2;		// eg. 5/2=2 => {-2, -1, 0, 1, 2}
			if (half == 0) half = 1;
			from = -half;
			to = half;
		}
		else
		{
			from = -numSetSize;
			to = numSetSize;
		}

		ValueSet result = new ValueSet();
		
		for (long a = from; a <= to; a++)
		{
			try
			{
				result.add(new IntegerValue(a));
			}
			catch (Exception e)
			{
				// Can't happen
			}
		}
		
		return result;
	}

	@Override
	public ValueSet caseRationalType(TCRationalType type, Integer limit)
	{
		return realLimit(limit);
	}

	@Override
	public ValueSet caseRealType(TCRealType type, Integer limit)
	{
		return realLimit(limit);
	}
	
	@Override
	public ValueSet caseFunctionType(TCFunctionType node, Integer arg)
	{
		throw new RuntimeException("Must define function bind range in VDM");
	}

	@Override
	public ValueSet caseNamedType(TCNamedType type, Integer limit)
	{
		if (done.contains(type))
		{
			return new ValueSet();		// recursing
		}
		
		ValueSet invs = new ValueSet();
		done.add(type);
		
		for (Value v: type.type.apply(this, limit))
		{
			try
			{
				invs.add(new InvariantValue(type, v, ctxt));
			}
			catch (ValueException e)
			{
				// Value does not match invariant, so ignore it
			}
		}
		
		done.remove(type);
		
		if (type.isOrdered(type.location))
		{
			Collections.sort(invs);
		}
		
		return invs;
	}
	
	@Override
	public ValueSet caseRecordType(TCRecordType node, Integer limit)
	{
		if (done.contains(node))
		{
			return new ValueSet();		// recursing
		}
		
		done.add(node);
		
		// Size will be the product of all fields, ie. limit ^ N. So we set root to the
		// Nth root of limit for each field (or 1, minimally).
		
		int root = (int) Math.floor(Math.pow(limit, 1.0D/node.fields.size()));
		if (root == 0) root = 1;
		
		ValueSet records = new ValueSet();
		List<ValueSet> fvalues = new Vector<ValueSet>(node.fields.size());
		int[] fsizes = new int[node.fields.size()];
		int f = 0;
		
		for (TCField field: node.fields)
		{
			ValueSet values = field.type.apply(this, root);
			fvalues.add(values);
			fsizes[f++] = values.size();
		}
		
		Selector p = new Selector(fsizes);
		int count = 0;
		
		for (int[] selection: p)
		{
			FieldMap map = new FieldMap();
			f = 0;
			
			for (TCField field: node.fields)
			{
				map.add(new FieldValue(field.tag, fvalues.get(f).get(selection[f]), field.equalityAbstraction));
				f++;
			}
			
			try
			{
				records.add(new RecordValue(node, map, ctxt));
				
				if (++count >= limit)
				{
					break;
				}
			}
			catch (ValueException e)
			{
				// Invariant omission
			}
		}

		done.remove(node);
		
		return records;
	}
	
	@Override
	public ValueSet caseSet1Type(TCSet1Type node, Integer limit)
	{
		ValueSet rs = new ValueSet();
		
		for (ValueSet vs: powerLimit(node.setof.apply(this, limit), limit, false))
		{
			rs.add(new SetValue(vs));
		}

		return rs;
	}
	
	@Override
	public ValueSet caseSetType(TCSetType node, Integer limit)
	{
		ValueSet rs = new ValueSet();
		
		for (ValueSet vs: powerLimit(node.setof.apply(this, limit), limit, true))
		{
			rs.add(new SetValue(vs));
		}

		return rs;
	}
	
	@Override
	public ValueSet caseSeq1Type(TCSeq1Type node, Integer limit)
	{
		ValueSet rs = new ValueSet();
		
		for (ValueSet vs: powerLimit(node.seqof.apply(this, limit), limit, false))
		{
			ValueList seq = new ValueList();
			seq.addAll(vs);
			rs.add(new SeqValue(seq));
		}

		return rs;
	}
	
	@Override
	public ValueSet caseSeqType(TCSeqType node, Integer limit)
	{
		ValueSet rs = new ValueSet();
		
		for (ValueSet vs: powerLimit(node.seqof.apply(this, limit), limit, true))
		{
			ValueList seq = new ValueList();
			seq.addAll(vs);
			rs.add(new SeqValue(seq));
		}

		return rs;
	}

	@Override
	public ValueSet caseMapType(TCMapType type, Integer limit)
	{
		ValueSet fromValues = type.from.apply(this, limit);
		ValueSet toValues = type.to.apply(this, limit);
		ValueSet results = new ValueSet();
		
		int fromSize = fromValues.size();
		int toSize = toValues.size();
		long count = 0;
		
		out: for (int ds=1; ds<=fromSize; ds++)		// map domain sizes
		{
			for (int[] domain: new KCombinator(fromSize, ds))
			{
				for (int[] range: new DuplicateKPermutor(toSize, ds))
				{
					ValueMap m = new ValueMap();

					for (int i=0; i<ds; i++)
					{
						m.put(fromValues.get(domain[i]), toValues.get(range[i]));
					}
					
					results.add(new MapValue(m));
					
					if (++count >= limit)
					{
						break out;
					}
				}
			}
		}
		
		results.add(new MapValue());	// empty map
		return results;
	}
	
	@Override
	public ValueSet caseInMapType(TCInMapType type, Integer limit)
	{
		ValueSet fromValues = type.from.apply(this, limit);
		ValueSet toValues = type.to.apply(this, limit);
		ValueSet results = new ValueSet();
		
		int fromSize = fromValues.size();
		int toSize = toValues.size();
		long count = 0;
		
		out: for (int ds=1; ds<=fromSize && ds<=toSize; ds++)		// map domain sizes
		{
			for (int[] domain: new KCombinator(fromSize, ds))
			{
				for (int[] range: new KPermutor(toSize, ds))
				{
					ValueMap m = new ValueMap();

					for (int i=0; i<ds; i++)
					{
						m.put(fromValues.get(domain[i]), toValues.get(range[i]));
					}
					
					results.add(new MapValue(m));
					
					if (++count >= limit)
					{
						break out;
					}
				}				
			}
		}
		
		results.add(new MapValue());	// empty map
		return results;
	}
	
	@Override
	public ValueSet caseProductType(TCProductType node, Integer limit)
	{
		// Size will be the product of all fields, ie. limit ^ N. So we set root to the
		// Nth root of limit for each field (or 1, minimally).
		
		int root = (int) Math.floor(Math.pow(limit, 1.0D/node.types.size()));
		if (root == 0) root = 1;
		
		ValueSet records = new ValueSet();
		List<ValueSet> fvalues = new Vector<ValueSet>(node.types.size());
		int[] fsizes = new int[node.types.size()];
		int f = 0;
		
		for (TCType field: node.types)
		{
			ValueSet values = field.apply(this, root);
			fvalues.add(values);
			fsizes[f++] = values.size();
		}
		
		Selector p = new Selector(fsizes);
		int count = 0;
		
		for (int[] selection: p)
		{
			ValueList tuple = new ValueList();
			
			for (int i=0; i < node.types.size(); i++)
			{
				tuple.add(fvalues.get(i).get(selection[i]));
			}
			
			records.add(new TupleValue(tuple));
			
			if (++count >= limit)
			{
				break;
			}
		}
		
		return records;
	}
	
	@Override
	public ValueSet caseUnionType(TCUnionType node, Integer limit)
	{
		ValueSet union = new ValueSet();
		
		for (TCType type: node.types)
		{
			union.addAll(type.apply(this, limit));
		}
		
		return union;
	}

	private ValueSet realLimit(Integer limit)
	{
		ValueSet result = new ValueSet();
		int from = -numSetSize;
		int to = numSetSize;
		
		for (double a = from; a <= to; a++)
		{
			for (double b = from; b <= to; b++)
			{
				if (b != 0)
				{
					try
					{
						result.add(new RealValue(a / b));
						
						if (result.size() >= limit)
						{
							return result;
						}
					}
					catch (Exception e)
					{
						// Can't be infinite or NaN
					}
				}
			}
		}
		
		return result;
	}

	private List<ValueSet> powerLimit(ValueSet set, int limit, boolean empty)
	{
		// Generate a power set, up to limit values from the full power set.
		List<ValueSet> results = new Vector<ValueSet>();
		
		if (set.isEmpty())
		{
			if (empty)
			{
				results.add(new ValueSet());	// Just {}
			}
		}
		else
		{
			/**
			 * The KCombinator below produces combinations in order (eg. [1,2] before [1,3]).
			 * And we loop the combination sizes from large to small, which is also the
			 * natural ordering for sets.
			 */
			int size = set.size();
			long count = 0;
			
			if (empty)
			{
				results.add(new ValueSet());	// Add {}
				count++;
			}
			
			for (int ss=size; ss>0; ss--)
			{
				for (int[] kc: new KCombinator(size, ss))
				{
					ValueSet ns = new ValueSet(ss);
	
					for (int i=0; i<ss; i++)
					{
						ns.add(set.get(kc[i]));
					}
					
					results.add(ns);
					
					if (++count >= limit)
					{
						return results;
					}
				}
			}
		}
	
		return results;
	}
}
