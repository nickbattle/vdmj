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
import java.util.Random;
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

public class RandomRangeCreator extends TCTypeVisitor<ValueSet, Integer>
{
	private final int numSetSize;	// {1, ..., N} for numerics
	private final Context ctxt;
	private final TCTypeSet done;
	private final Random prng;
	
	public RandomRangeCreator(Context ctxt, int numSetSize, long seed)
	{
		this.ctxt = ctxt;
		this.numSetSize = numSetSize;
		this.done = new TCTypeSet();
		this.prng = new Random(seed);
	}
	
	private int nextNat(int bound)
	{
		int n = -1;
		while (n < 0) n = prng.nextInt(bound);
		return n;
	}
	
//	private int nextNat1(int bound)
//	{
//		int n = -1;
//		while (n <= 0) n = prng.nextInt(bound);
//		return n;
//	}

	private int nextNat()
	{
		int n = -1;
		while (n < 0) n = prng.nextInt();
		return n;
	}
	
	private int nextNat1()
	{
		int n = -1;
		while (n <= 0) n = prng.nextInt();
		return n;
	}

	@Override
	public ValueSet caseType(TCType type, Integer limit)
	{
		throw new RuntimeException("Missing RandomRangeCreator case for " + type);
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
			case 1:		return new ValueSet(new BooleanValue(prng.nextBoolean()));
			default:	return new ValueSet(new BooleanValue(true), new BooleanValue(false));
		}
	}
	
	@Override
	public ValueSet caseCharacterType(TCCharacterType node, Integer limit)
	{
		String alphabet = "abcdefghijklmnopqrstuvwxyz";
		
		switch (limit)
		{
			case 0:		return new ValueSet();
			case 1:		return new ValueSet(new CharacterValue(alphabet.charAt(prng.nextInt(alphabet.length()))));
			default:
				ValueSet result = new ValueSet();
				
				for (int i=0; i < limit; i++)
				{
					char c = alphabet.charAt(prng.nextInt(alphabet.length()));
					result.add(new CharacterValue(c));
				}
				
				return result;
		}
	}
	
	@Override
	public ValueSet caseTokenType(TCTokenType node, Integer limit)
	{
		switch (limit)
		{
			case 0:		return new ValueSet();
			case 1:		return new ValueSet(new TokenValue(new IntegerValue(1)));
			default:
				ValueSet result = new ValueSet();
				
				for (int i=0; i < limit; i++)
				{
					result.add(new TokenValue(new IntegerValue(prng.nextInt())));
				}
				
				return result;
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
		ValueSet result = new ValueSet();
		long num = limit < numSetSize ? limit : numSetSize;
		
		for (long a = 0; a < num; a++)
		{
			try
			{
				result.add(new NaturalOneValue(nextNat1()));
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
		ValueSet result = new ValueSet();
		long num = limit < numSetSize ? limit : numSetSize;
		
		for (long a = 0; a < num; a++)
		{
			try
			{
				result.add(new NaturalValue(nextNat()));
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
		ValueSet result = new ValueSet();
		long num = limit < numSetSize ? limit : numSetSize;
		
		for (long a = 0; a < num; a++)
		{
			try
			{
				result.add(new IntegerValue(prng.nextInt()));
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
	public ValueSet caseFunctionType(TCFunctionType node, Integer limit)
	{
		return new ValueSet();	// Can't generate functions!
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
		long num = limit < numSetSize ? limit : numSetSize;
		
		for (long a = 0; a < num; a++)
		{
			try
			{
				double n = prng.nextDouble();
				result.add(new RealValue(n));
			}
			catch (Exception e)
			{
				// Can't happen
			}
		}
		
		return result;
	}

	private List<ValueSet> powerLimit(ValueSet source, int limit, boolean incEmpty)
	{
		// Generate a power set, up to limit values from the full power set.
		List<ValueSet> results = new Vector<ValueSet>();
		
		if (source.isEmpty())
		{
			if (incEmpty)
			{
				results.add(new ValueSet());	// Just {}
			}
		}
		else
		{
			int size = source.size();
			long count = 0;
			
			if (incEmpty)
			{
				results.add(new ValueSet());	// Add {}
				count++;
			}
			
			for (int ss: randomOrder(size))
			{
				for (int[] kc: new KCombinator(size, ss))
				{
					ValueSet ns = new ValueSet(ss);
	
					for (int i=0; i<ss; i++)
					{
						ns.addNoSort(source.get(kc[i]));	// KComb is sorted already
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
	
	private int[] randomOrder(int size)
	{
		int[] result = new int[size];
		
		for (int i=0; i<size; i++)
		{
			result[i] = i+1;
		}
		
		for (int j=0; j<size; j++)	// jumble size times
		{
			int a = nextNat(size);
			int b = nextNat(size);
			
			int temp = result[a];
			result[a] = result[b];
			result[b] = temp;
		}
		
		return result;
	}
}
