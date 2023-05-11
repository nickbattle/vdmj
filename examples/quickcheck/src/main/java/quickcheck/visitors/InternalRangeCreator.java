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

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.util.Selector;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CharacterValue;
import com.fujitsu.vdmj.values.FieldMap;
import com.fujitsu.vdmj.values.FieldValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.InvariantValue;
import com.fujitsu.vdmj.values.NaturalOneValue;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.NilValue;
import com.fujitsu.vdmj.values.QuoteValue;
import com.fujitsu.vdmj.values.RealValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.TokenValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class InternalRangeCreator extends TCTypeVisitor<ValueList, Integer>
{
	private final int NUMERIC_LIMIT;
	private final Context ctxt;
	private final TCTypeSet done;
	
	public InternalRangeCreator(Context ctxt, int limit)
	{
		this.ctxt = ctxt;
		this.NUMERIC_LIMIT = limit;
		this.done = new TCTypeSet();
	}

	@Override
	public ValueList caseType(TCType type, Integer limit)
	{
		throw new RuntimeException("Missing InternalRangeCreator case for " + type);
	}

	@Override
	public ValueList caseBooleanType(TCBooleanType type, Integer limit)
	{
		switch (limit)
		{
			case 0:		return new ValueList();
			case 1:		return new ValueList(new BooleanValue(false));
			default:	return new ValueList(new BooleanValue(true), new BooleanValue(false));
		}
	}
	
	@Override
	public ValueList caseCharacterType(TCCharacterType node, Integer limit)
	{
		switch (limit)
		{
			case 0:		return new ValueList();
			case 1:		return new ValueList(new CharacterValue('a'));
			default:	return new ValueList(new CharacterValue('a'), new CharacterValue('b'));
		}
	}
	
	@Override
	public ValueList caseTokenType(TCTokenType node, Integer limit)
	{
		switch (limit)
		{
			case 0:		return new ValueList();
			case 1:		return new ValueList(new TokenValue(new IntegerValue(1)));
			default:	return new ValueList(new TokenValue(new IntegerValue(1)), new TokenValue(new IntegerValue(2)));
		}
	}
	
	@Override
	public ValueList caseOptionalType(TCOptionalType node, Integer limit)
	{
		switch (limit)
		{
			case 0:		return new ValueList();
			case 1:		return new ValueList(new NilValue());
			default:
				ValueList list = node.type.apply(this, limit - 1);
				list.add(new NilValue());
				return list;
		}
	}
	
	@Override
	public ValueList caseBracketType(TCBracketType node, Integer limit)
	{
		return node.type.apply(this, limit);
	}

	@Override
	public ValueList caseQuoteType(TCQuoteType node, Integer limit)
	{
		return new ValueList(new QuoteValue(node.value));
	}
	
	@Override
	public ValueList caseNaturalOneType(TCNaturalOneType node, Integer limit)
	{
		int to = NUMERIC_LIMIT;

		if (limit < NUMERIC_LIMIT)
		{
			to = limit;
		}

		ValueList result = new ValueList();
		
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
	public ValueList caseNaturalType(TCNaturalType node, Integer limit)
	{
		int to = NUMERIC_LIMIT;
		
		if (limit < NUMERIC_LIMIT)
		{
			to = limit;
		}

		ValueList result = new ValueList();
		
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
	public ValueList caseIntegerType(TCIntegerType node, Integer limit)
	{
		int from = 0;
		int to = 0;
		
		if (limit < NUMERIC_LIMIT * 2 + 1)
		{
			int half = limit / 2;		// eg. 5/2=2 => {-2, -1, 0, 1, 2}
			if (half == 0) half = 1;
			from = -half;
			to = half;
		}
		else
		{
			from = -NUMERIC_LIMIT;
			to = NUMERIC_LIMIT;
		}

		ValueList result = new ValueList();
		
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
	public ValueList caseRationalType(TCRationalType type, Integer limit)
	{
		return realLimit(limit);
	}

	@Override
	public ValueList caseRealType(TCRealType type, Integer limit)
	{
		return realLimit(limit);
	}
	
	private ValueList realLimit(Integer limit)
	{
		ValueList result = new ValueList();
		int from = 0;
		int to = 0;
		
		if (limit < NUMERIC_LIMIT * NUMERIC_LIMIT)
		{
			int half = (int) Math.round(Math.sqrt(limit)) / 2;
			if (half == 0) half = 1;
			from = -half;
			to = half;
		}
		else
		{
			from = -NUMERIC_LIMIT;
			to = NUMERIC_LIMIT;
		}
		
		for (double a = from; a <= to; a++)
		{
			for (double b = from; b <= to; b++)
			{
				if (b != 0)
				{
					try
					{
						result.add(new RealValue(a / b));
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
	
	@Override
	public ValueList caseFunctionType(TCFunctionType node, Integer arg)
	{
		throw new RuntimeException("Must define function bind range in VDM");
	}

	@Override
	public ValueList caseNamedType(TCNamedType type, Integer limit)
	{
		if (done.contains(type))
		{
			return new ValueList();		// recursing
		}
		
		ValueList invs = new ValueList();
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
	public ValueList caseRecordType(TCRecordType node, Integer limit)
	{
		if (done.contains(node))
		{
			return new ValueList();		// recursing
		}
		
		done.add(node);
		
		// Size will be the product of all fields, ie. limit ^ N. So we set root to the
		// Nth root of limit for each field (or 1, minimally).
		
		int root = (int) Math.floor(Math.pow(limit, 1/node.fields.size()));
		if (root == 0) root = 1;
		
		ValueList records = new ValueList();
		List<ValueList> fvalues = new Vector<ValueList>(node.fields.size());
		int[] fsizes = new int[fvalues.size()];
		int f = 0;
		
		for (TCField field: node.fields)
		{
			ValueList values = field.type.apply(this, root);
			fvalues.add(values);
			fsizes[f++] = fvalues.size();
		}
		
		Selector p = new Selector(fsizes);
		
		for (int[] selection: p)
		{
			FieldMap map = new FieldMap();
			f = 0;
			
			for (TCField field: node.fields)
			{
				map.add(new FieldValue(field.tag, fvalues.get(f).get(selection[f]), field.equalityAbstraction));
			}
			
			try
			{
				records.add(new RecordValue(node, map, ctxt));
			}
			catch (ValueException e)
			{
				// Invariant omission
			}
		}
		
		done.remove(node);
		
		return records;
	}
}
