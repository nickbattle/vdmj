/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

import java.util.Collections;
import java.util.List;

import com.fujitsu.vdmj.in.patterns.INIdentifierPattern;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
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
import com.fujitsu.vdmj.util.DuplicateKPermutor;
import com.fujitsu.vdmj.util.KCombinator;
import com.fujitsu.vdmj.util.KPermutor;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.InvariantValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NilValue;
import com.fujitsu.vdmj.values.ParameterValue;
import com.fujitsu.vdmj.values.Quantifier;
import com.fujitsu.vdmj.values.QuantifierList;
import com.fujitsu.vdmj.values.QuoteValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueMap;
import com.fujitsu.vdmj.values.ValueSet;

public class INGetAllValuesVisitor extends TCTypeVisitor<ValueList, Context>
{
	@Override
	public ValueList caseType(TCType type, Context arg)
	{
		throw new RuntimeException("Missing INGetAllValuesVisitor case for " + type);
	}

	@Override
	public ValueList caseBooleanType(TCBooleanType type, Context ctxt)
	{
		ValueList v = new ValueList();
		v.add(new BooleanValue(true));
		v.add(new BooleanValue(false));
		return v;
	}

	@Override
	public ValueList caseBracketType(TCBracketType type, Context ctxt)
	{
		return type.type.apply(this, ctxt);
	}

	@Override
	public ValueList caseInMapType(TCInMapType type, Context ctxt)
	{
		ValueList fromValues = type.from.apply(this, ctxt);
		ValueList toValues = type.to.apply(this, ctxt);
		ValueList results = new ValueList();
		
		int fromSize = fromValues.size();
		int toSize = toValues.size();
		
		for (int ds=1; ds<=fromSize && ds<=toSize; ds++)		// map domain sizes
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
				}
				
				checkBreakpoint(type, new Breakpoint(ctxt.location), ctxt);
			}
		}
		
		results.add(new MapValue());	// empty map
		return results;
	}

	@Override
	public ValueList caseMapType(TCMapType type, Context ctxt)
	{
		ValueList fromValues = type.from.apply(this, ctxt);
		ValueList toValues = type.to.apply(this, ctxt);
		ValueList results = new ValueList();
		
		int fromSize = fromValues.size();
		int toSize = toValues.size();
		
		for (int ds=1; ds<=fromSize; ds++)		// map domain sizes
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
				}
				
				checkBreakpoint(type, new Breakpoint(ctxt.location), ctxt);
			}
		}
		
		results.add(new MapValue());	// empty map
		return results;
	}

	@Override
	public ValueList caseNamedType(TCNamedType type, Context ctxt)
	{
		ValueList invs = new ValueList();
		
		for (Value v: type.type.apply(this, ctxt))
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
		
		if (type.isOrdered(type.location))
		{
			Collections.sort(invs);
		}
		
		return invs;
	}

	@Override
	public ValueList caseOptionalType(TCOptionalType type, Context ctxt)
	{
		ValueList list = type.type.apply(this, ctxt);
		list.add(new NilValue());
		return list;
	}

	@Override
	public ValueList caseParameterType(TCParameterType type, Context ctxt)
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
	public ValueList caseProductType(TCProductType type, Context ctxt)
	{
		return ofTypeList(type.types, ctxt);
	}

	@Override
	public ValueList caseQuoteType(TCQuoteType type, Context ctxt)
	{
		ValueList v = new ValueList();
		v.add(new QuoteValue(type.value));
		return v;
	}

	@Override
	public ValueList caseRecordType(TCRecordType type, Context ctxt)
	{
		TCTypeList fieldtypes = new TCTypeList();

		for (TCField f: type.fields)
		{
			fieldtypes.add(f.type);
		}

		ValueList results = new ValueList();

		for (Value v: ofTypeList(fieldtypes, ctxt))
		{
			try
			{
				TupleValue tuple = (TupleValue)v;
				results.add(new RecordValue(type, tuple.values, ctxt));
			}
			catch (ValueException e)
			{
				// Value does not match invariant, so ignore it
			}
		}
		
		if (type.isOrdered(type.location))
		{
			Collections.sort(results);
		}

		return results;
	}

	@Override
	public ValueList caseSetType(TCSetType type, Context ctxt)
	{
		ValueList list = type.setof.apply(this, ctxt);
		ValueSet set = new ValueSet(list.size());
		set.addAll(list);
		List<ValueSet> psets = set.powerSet(new Breakpoint(ctxt.location), ctxt);
		list.clear();

		for (ValueSet v: psets)
		{
			list.add(new SetValue(v));
		}
		
		if (type instanceof TCSet1Type)
		{
			list.remove(new SetValue());  // Remove {}
		}

		return list;
	}

	@Override
	public ValueList caseUnionType(TCUnionType type, Context ctxt)
	{
		ValueList v = new ValueList();

		for (TCType member: type.types)
		{
			v.addAll(member.apply(this, ctxt));
		}

		return v;
	}
	
	/**
	 * The remaining types are "infinite" and all produce an error.
	 */
	
	@Override
	public ValueList caseCharacterType(TCCharacterType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}
	
	@Override
	public ValueList caseClassType(TCClassType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseFunctionType(TCFunctionType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseIntegerType(TCIntegerType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseNaturalOneType(TCNaturalOneType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseNaturalType(TCNaturalType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseNumericType(TCNumericType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseOperationType(TCOperationType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseRationalType(TCRationalType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseRealType(TCRealType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseSeqType(TCSeqType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseTokenType(TCTokenType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseUndefinedType(TCUndefinedType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseUnknownType(TCUnknownType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseUnresolvedType(TCUnresolvedType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseVoidReturnType(TCVoidReturnType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	@Override
	public ValueList caseVoidType(TCVoidType type, Context ctxt)
	{
		return infiniteType(type, ctxt);
	}

	/**
	 * Throw a runtime exception wrapping a ValueException cause. We need to add some sort
	 * of exception mechanism to visitors? 
	 */
	private ValueList infiniteType(TCType type, Context ctxt)
	{
		throw new InternalException(4, "Cannot get bind values for type " + type);
	}
	
	private ValueList ofTypeList(TCTypeList types, Context ctxt)
	{
		QuantifierList quantifiers = new QuantifierList();
		int n = 0;

		for (TCType t: types)
		{
			TCNameToken name = new TCNameToken(t.location, "#", String.valueOf(n));
			INPattern p = new INIdentifierPattern(name);
			Quantifier q = new Quantifier(p, t.apply(this, ctxt));
			quantifiers.add(q);
		}

		quantifiers.init(ctxt, true);
		ValueList results = new ValueList();

		while (quantifiers.hasNext())
		{
			NameValuePairList nvpl = quantifiers.next();
			ValueList list = new ValueList();

			for (NameValuePair nvp: nvpl)
			{
				list.add(nvp.value);
			}
			
			results.add(new TupleValue(list));
		}
		
		return results;
	}
	
	/**
	 * Check whether we should drop into the debugger for long expansions.
	 */
	private void checkBreakpoint(TCType type, Breakpoint breakpoint, Context ctxt)
	{
		// We check the interrupt level here, rather than letting the check
		// method do it, to avoid incrementing the hit count for the breakpoint
		// too many times.

		switch (Breakpoint.execInterruptLevel())
		{
			case Breakpoint.TERMINATE:
				long size = type.apply(new INTypeSizeVisitor(), ctxt);
				throw new InternalException(4176, "Interrupted type expansion size " + size);
		
			case Breakpoint.PAUSE:
				if (breakpoint != null)
				{
					breakpoint.enterDebugger(ctxt);
				}
				break;
			
			case Breakpoint.NONE:
			default:
				break;	// carry on
		}
	}
}
