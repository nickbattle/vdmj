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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueMap;

public class INMapSeqDesignator extends INStateDesignator
{
	private static final long serialVersionUID = 1L;
	public final INStateDesignator mapseq;
	public final INExpression exp;

	public final TCMapType mapType;
	public final TCSeqType seqType;

	public INMapSeqDesignator(INStateDesignator mapseq, INExpression exp, TCMapType mapType, TCSeqType seqType)
	{
		super(mapseq.location);
		this.mapseq = mapseq;
		this.exp = exp;
		this.mapType = mapType;
		this.seqType = seqType;
	}

	@Override
	public String toString()
	{
		return mapseq + "(" + exp + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		Value result = null;

		try
		{
			Value root = mapseq.eval(ctxt);
			Value index = exp.eval(ctxt);

			if (root.isType(MapValue.class))
			{
				index = index.convertTo(mapType.from, ctxt);
				ValueMap map = root.mapValue(ctxt);
				result = map.get(index);

				if (result == null && root instanceof UpdatableValue)
				{
					// Assignment to a non-existent map key creates the value
					// in order to have it updated.

					UpdatableValue ur = (UpdatableValue)root;
					result = UpdatableValue.factory(ur.listeners, mapType.to);
					map.put(index, result);
				}
			}
			else if (root.isType(SeqValue.class))
			{
				ValueList seq = root.seqValue(ctxt);
				int i = (int)index.intValue(ctxt)-1;

				if (!seq.inbounds(i))
				{
					if (i == seq.size())
					{
						// Assignment to an index one greater than the length
						// creates the value in order to have it updated.

						UpdatableValue ur = (UpdatableValue)root;
						seq.add(UpdatableValue.factory(ur.listeners, seqType.seqof));
					}
					else
					{
						exp.abort(4019, "Sequence cannot extend to key: " + index, ctxt);
					}
				}

				result = seq.get(i);
			}
			else
			{
				abort(4020, "State value is neither a sequence nor a map", ctxt);
			}
		}
		catch (ValueException e)
		{
			abort(e);
		}

		return result;
	}
}
