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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.types;

import java.util.List;

import com.fujitsu.vdmj.in.patterns.INIdentifierPattern;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCInMapType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.values.BooleanValue;
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

/**
 * This class is a compromise between having a tree of "IN" classes for all of the TCTypes just
 * to provide the getAllValues functionality, and implementing a visitor just for this purpose. 
 */
public class GetAllValues
{
	public static ValueList ofType(TCType type, Context ctxt) throws ValueException
	{
		if (type instanceof TCBooleanType)
		{
			return getTCBooleanType();
		}
		else if (type instanceof TCBracketType)
		{
			return getTCBracketType((TCBracketType)type, ctxt);
		}
//		else if (type instanceof TCCharacterType)
//		{
//			return getTCCharacterType((TCCharacterType)type, ctxt);
//		}
//		else if (type instanceof TCClassType)
//		{
//			return getTCClassType((TCClassType)type, ctxt);
//		}
//		else if (type instanceof TCFunctionType)
//		{
//			return getTCFunctionType((TCFunctionType)type, ctxt);
//		}
		else if (type instanceof TCInMapType)
		{
			return getTCInMapType((TCInMapType)type, ctxt);
		}
//		else if (type instanceof TCIntegerType)
//		{
//			return getTCIntegerType((TCIntegerType)type, ctxt);
//		}
		else if (type instanceof TCMapType)
		{
			return getTCMapType((TCMapType)type, ctxt);
		}
		else if (type instanceof TCNamedType)
		{
			return getTCNamedType((TCNamedType)type, ctxt);
		}
//		else if (type instanceof TCNaturalOneType)
//		{
//			return getTCNaturalOneType((TCNaturalOneType)type, ctxt);
//		}
//		else if (type instanceof TCNaturalType)
//		{
//			return getTCNaturalType((TCNaturalType)type, ctxt);
//		}
//		else if (type instanceof TCNumericType)
//		{
//			return getTCNumericType((TCNumericType)type, ctxt);
//		}
//		else if (type instanceof TCOperationType)
//		{
//			return getTCOperationType((TCOperationType)type, ctxt);
//		}
		else if (type instanceof TCOptionalType)
		{
			return getTCOptionalType((TCOptionalType)type, ctxt);
		}
		else if (type instanceof TCParameterType)
		{
			return getTCParameterType((TCParameterType)type, ctxt);
		}
		else if (type instanceof TCProductType)
		{
			return getTCProductType((TCProductType)type, ctxt);
		}
		else if (type instanceof TCQuoteType)
		{
			return getTCQuoteType((TCQuoteType)type);
		}
//		else if (type instanceof TCRationalType)
//		{
//			return getTCRationalType((TCRationalType)type, ctxt);
//		}
//		else if (type instanceof TCRealType)
//		{
//			return getTCRealType((TCRealType)type, ctxt);
//		}
		else if (type instanceof TCRecordType)
		{
			return getTCRecordType((TCRecordType)type, ctxt);
		}
//		else if (type instanceof TCSeqType)
//		{
//			return getTCSeqType((TCSeqType)type, ctxt);
//		}
		else if (type instanceof TCSetType)
		{
			return getTCSetType((TCSetType)type, ctxt);
		}
//		else if (type instanceof TCTokenType)
//		{
//			return getTCTokenType((TCTokenType)type, ctxt);
//		}
//		else if (type instanceof TCUndefinedType)
//		{
//			return getTCUndefinedType((TCUndefinedType)type, ctxt);
//		}
		else if (type instanceof TCUnionType)
		{
			return getTCUnionType((TCUnionType)type, ctxt);
		}
//		else if (type instanceof TCUnknownType)
//		{
//			return getTCUnknownType((TCUnknownType)type, ctxt);
//		}
//		else if (type instanceof TCUnresolvedType)
//		{
//			return getTCUnresolvedType((TCUnresolvedType)type, ctxt);
//		}
//		else if (type instanceof TCVoidReturnType)
//		{
//			return getTCVoidReturnType((TCVoidReturnType)type, ctxt);
//		}
//		else if (type instanceof TCVoidType)
//		{
//			return getTCVoidType((TCVoidType)type, ctxt);
//		}
		else
		{
			throw new ValueException(4, "Cannot get bind values for type " + type, ctxt);
		}
	}

	private static ValueList getTCBooleanType()
	{
		ValueList v = new ValueList();
		v.add(new BooleanValue(true));
		v.add(new BooleanValue(false));
		return v;
	}

	private static ValueList getTCBracketType(TCBracketType type, Context ctxt) throws ValueException
	{
		return ofType(type.type, ctxt);
	}

//	private static ValueList getTCCharacterType(TCCharacterType type, Context ctxt)
//	{
//		return null;
//	}
//
//	private static ValueList getTCClassType(TCClassType type, Context ctxt)
//	{
//		return null;
//	}
//
//	private static ValueList getTCFunctionType(TCFunctionType type, Context ctxt)
//	{
//		return null;
//	}

	private static ValueList getTCInMapType(TCInMapType type, Context ctxt) throws ValueException
	{
		ValueList maps = getTCMapType(type, ctxt);
		ValueList result = new ValueList();
		
		for (Value map: maps)
		{
			MapValue vm = (MapValue)map;
			
			if (vm.values.isInjective())
			{
				result.add(vm);
			}
		}
		
		return result;
	}

//	private static ValueList getTCIntegerType(TCIntegerType type, Context ctxt)
//	{
//		return null;
//	}

	private static ValueList getTCMapType(TCMapType type, Context ctxt) throws ValueException
	{
		TCTypeList tuple = new TCTypeList();
		tuple.add(type.from);
		tuple.add(type.to);
		
		ValueList results = new ValueList();
		ValueList tuples = ofTypeList(tuple, ctxt);
		ValueSet set = new ValueSet();
		set.addAll(tuples);
		List<ValueSet> psets = set.powerSet();

		for (ValueSet map: psets)
		{
			ValueMap result = new ValueMap();
			
			for (Value v: map)
			{
				TupleValue tv = (TupleValue)v;
				result.put(tv.values.get(0), tv.values.get(1));
			}
			
			results.add(new MapValue(result));
		}
		
		return results; 
	}

	private static ValueList getTCNamedType(TCNamedType type, Context ctxt) throws ValueException
	{
		return ofType(type.type, ctxt);
	}

//	private static ValueList getTCNaturalOneType(TCNaturalOneType type, Context ctxt)
//	{
//		return null;
//	}
//
//	private static ValueList getTCNaturalType(TCNaturalType type, Context ctxt)
//	{
//		return null;
//	}
//
//	private static ValueList getTCNumericType(TCNumericType type, Context ctxt)
//	{
//		return null;
//	}
//
//	private static ValueList getTCOperationType(TCOperationType type, Context ctxt)
//	{
//		return null;
//	}

	private static ValueList getTCOptionalType(TCOptionalType type, Context ctxt) throws ValueException
	{
		ValueList list = ofType(type.type, ctxt);
		list.add(new NilValue());
		return list;
	}

	private static ValueList getTCParameterType(TCParameterType type, Context ctxt) throws ValueException
	{
		Value t = ctxt.lookup(type.name);

		if (t == null)
		{
			ExceptionHandler.abort(type.location, 4008, "No such type parameter @" + type.name + " in scope", ctxt);
		}
		else if (t instanceof ParameterValue)
		{
			ParameterValue tv = (ParameterValue)t;
			return ofType(tv.type, ctxt);
		}
		
		ExceptionHandler.abort(type.location, 4009, "Type parameter/local variable name clash, @" + type.name, ctxt);
		return null;
	}

	private static ValueList getTCProductType(TCProductType type, Context ctxt) throws ValueException
	{
		return ofTypeList(type.types, ctxt);
	}

	private static ValueList getTCQuoteType(TCQuoteType type)
	{
		ValueList v = new ValueList();
		v.add(new QuoteValue(type.value));
		return v;
	}

//	private static ValueList getTCRationalType(TCRationalType type, Context ctxt)
//	{
//		return null;
//	}
//
//	private static ValueList getTCRealType(TCRealType type, Context ctxt)
//	{
//		return null;
//	}

	private static ValueList getTCRecordType(TCRecordType type, Context ctxt) throws ValueException
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

		return results;
	}

//	private static ValueList getTCSeqType(TCSeqType type, Context ctxt)
//	{
//		return null;
//	}

	private static ValueList getTCSetType(TCSetType type, Context ctxt) throws ValueException
	{
		ValueList list = ofType(type.setof, ctxt);
		ValueSet set = new ValueSet(list.size());
		set.addAll(list);
		List<ValueSet> psets = set.powerSet();
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

//	private static ValueList getTCTokenType(TCTokenType type, Context ctxt)
//	{
//		return null;
//	}
//
//	private static ValueList getTCUndefinedType(TCUndefinedType type, Context ctxt)
//	{
//		return null;
//	}

	private static ValueList getTCUnionType(TCUnionType type, Context ctxt) throws ValueException
	{
		ValueList v = new ValueList();

		for (TCType member: type.types)
		{
			v.addAll(ofType(member, ctxt));
		}

		return v;
	}

//	private static ValueList getTCUnknownType(TCUnknownType type, Context ctxt)
//	{
//		return null;
//	}
//
//	private static ValueList getTCUnresolvedType(TCUnresolvedType type, Context ctxt)
//	{
//		return null;
//	}
//
//	private static ValueList getTCVoidReturnType(TCVoidReturnType type, Context ctxt)
//	{
//		return null;
//	}
//
//	private static ValueList getTCVoidType(TCVoidType type, Context ctxt)
//	{
//		return null;
//	}
	
	private static ValueList ofTypeList(TCTypeList types, Context ctxt) throws ValueException
	{
		QuantifierList quantifiers = new QuantifierList();
		int n = 0;

		for (TCType t: types)
		{
			TCNameToken name = new TCNameToken(t.location, "#", String.valueOf(n));
			INPattern p = new INIdentifierPattern(name);
			Quantifier q = new Quantifier(p, ofType(t, ctxt));
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
}
