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

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.InvariantValue;
import com.fujitsu.vdmj.values.RealValue;
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
	public ValueList caseRealType(TCRealType type, Integer limit)
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
	public ValueList caseNamedType(TCNamedType type, Integer maxsize)
	{
		if (done.contains(type))
		{
			return new ValueList();		// recursing
		}
		
		ValueList invs = new ValueList();
		done.add(type);
		
		for (Value v: type.type.apply(this, maxsize))
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
}
