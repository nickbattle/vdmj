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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCMapSeqDesignator extends TCStateDesignator
{
	private static final long serialVersionUID = 1L;
	public final TCStateDesignator mapseq;
	public final TCExpression exp;

	private TCMapType mapType;
	private TCSeqType seqType;

	public TCMapSeqDesignator(TCStateDesignator mapseq, TCExpression exp)
	{
		super(mapseq.location);
		this.mapseq = mapseq;
		this.exp = exp;
	}

	@Override
	public String toString()
	{
		return mapseq + "(" + exp + ")";
	}

	@Override
	public TCType typeCheck(Environment env)
	{
		TCType etype = exp.typeCheck(env, null, NameScope.NAMESANDSTATE, null);
		TCType rtype = mapseq.typeCheck(env);
		TCTypeSet result = new TCTypeSet();

		if (rtype.isMap(location))
		{
			mapType = rtype.getMap();

			if (!TypeComparator.compatible(mapType.from, etype))
			{
				report(3242, "Map element assignment of wrong type");
				detail2("Expect", mapType.from, "Actual", etype);
			}
			else
			{
				result.add(mapType.to);
			}
		}

		if (rtype.isSeq(location))
		{
			seqType = rtype.getSeq();

			if (!etype.isNumeric(location))
			{
				report(3243, "Seq index is not numeric");
				detail("Actual", etype);
			}
			else
			{
				result.add(seqType.seqof);
			}
		}
		
		if (rtype.isFunction(location))
		{
			// Error case, but improves errors if we work out the return type
			TCFunctionType ftype = rtype.getFunction();
			result.add(ftype.result);
		}

		if (rtype.isOperation(location))
		{
			// Error case, but improves errors if we work out the return type
			TCOperationType otype = rtype.getOperation();
			result.add(otype.result);
		}

		if (result.isEmpty())
		{
			report(3244, "Expecting a map or a sequence");
			return new TCUnknownType(location);
		}

		return result.getType(location);
	}
}
