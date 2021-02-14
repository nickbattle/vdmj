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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCPlusPlusExpression extends TCBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCPlusPlusExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCType mapcons = null;
		TCType leftcons = null;
		
		if (constraint != null && constraint.isSeq(location))
		{
			TCSeqType st = constraint.getSeq();
			mapcons = new TCMapType(location, new TCNaturalOneType(location), st.seqof);
			leftcons = new TCSeqType(location);
		}
		else if (constraint != null && constraint.isMap(location))
		{
			TCMapType mt = constraint.getMap();
			mapcons = mt;
			leftcons = new TCMapType(location, mt.from, new TCUnknownType(location));
		}
		
		ltype = left.typeCheck(env, null, scope, leftcons);
		rtype = right.typeCheck(env, null, scope, mapcons);

		TCTypeSet result = new TCTypeSet();
		boolean unique = (!ltype.isUnion(location) && !rtype.isUnion(location));

		if (ltype.isMap(location))
		{
    		if (!rtype.isMap(location))
    		{
    			concern(unique, 3141, "Right hand of '++' is not a map");
    			detail(unique, "Type", rtype);
    			return new TCMapType(location);	// Unknown types
    		}

    		TCMapType lm = ltype.getMap();
    		TCMapType rm = rtype.getMap();

    		TCTypeSet domain = new TCTypeSet(lm.from, rm.from);
    		TCTypeSet range = new TCTypeSet(lm.to, rm.to);

    		result.add(new TCMapType(location,
    			domain.getType(location), range.getType(location)));
		}

		if (ltype.isSeq(location))
		{
    		TCSeqType st = ltype.getSeq();

    		if (!rtype.isMap(location))
    		{
    			concern(unique, 3142, "Right hand of '++' is not a map");
    			detail(unique, "Type", rtype);
    			result.add(st);
    		}
    		else
    		{
        		TCMapType mr = rtype.getMap();

        		if (!mr.from.isType(TCNumericType.class, location))
        		{
        			concern(unique, 3143, "Domain of right hand of '++' must be nat1");
        			detail(unique, "Type", mr.from);
        		}
        		
        		TCTypeSet type = new TCTypeSet(st.seqof, mr.to);
        		result.add(new TCSeqType(location, type.getType(location)));
    		}
		}

		if (result.isEmpty())
		{
			report(3144, "Left of '++' is neither a map nor a sequence");
			return new TCUnknownType(location);
		}

		return result.getType(location);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.casePlusPlusExpression(this, arg);
	}
}
