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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.patterns.POIgnorePattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.pog.ValueBindingObligation;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * A class to hold a value definition.
 */
public class POValueDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final POPattern pattern;
	public final TCType type;
	public final POExpression exp;
	public final TCType expType;

	public POValueDefinition(POPattern p, TCType type, POExpression exp, TCType expType)
	{
		super(p.location, null);
		this.pattern = p;
		this.type = type;
		this.exp = exp;
		this.expType = expType;
	}

	@Override
	public String toString()
	{
		return pattern + (type == null ? "" : ":" + type) + " = " + exp;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof POValueDefinition)
		{
			POValueDefinition vdo = (POValueDefinition)other;
			return vdo.pattern.equals(pattern) && vdo.type.equals(type) && vdo.exp.equals(exp);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public TCType getType()
	{
		return type;
	}

	@Override
	public TCNameList getVariableNames()
	{
		return pattern.getVariableNames();
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList list = exp.getProofObligations(ctxt);

		if (!(pattern instanceof POIdentifierPattern) &&
			!(pattern instanceof POIgnorePattern) &&
			type.isUnion(location))
		{
			TCType patternType = pattern.getPossibleType();	// With unknowns
			TCUnionType ut = type.getUnion();
			TCTypeSet set = new TCTypeSet();

			for (TCType u: ut.types)
			{
				if (TypeComparator.compatible(u, patternType))
				{
					set.add(u);
				}
			}

			if (!set.isEmpty())
			{
    			TCType compatible = set.getType(location);

    			if (!TypeComparator.isSubType(type, compatible))
    			{
    				list.add(new ValueBindingObligation(this, ctxt));
    				list.add(new SubTypeObligation(exp, compatible, type, ctxt));
    			}
			}
		}

		if (!TypeComparator.isSubType(ctxt.checkType(exp, expType), type))
		{
			list.add(new SubTypeObligation(exp, type, expType, ctxt));
		}

		return list;
	}
}
