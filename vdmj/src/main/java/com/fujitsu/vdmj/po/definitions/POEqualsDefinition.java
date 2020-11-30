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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POBind;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.patterns.POIgnorePattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POSeqBind;
import com.fujitsu.vdmj.po.patterns.POSetBind;
import com.fujitsu.vdmj.po.patterns.POTypeBind;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SeqMemberObligation;
import com.fujitsu.vdmj.pog.SetMemberObligation;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.pog.ValueBindingObligation;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * A class to hold an equals definition.
 */
public class POEqualsDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final POPattern pattern;
	public final POTypeBind typebind;
	public final POBind bind;
	public final POExpression test;

	public final TCType expType;
	public final TCType defType;
	public final PODefinitionList defs;

	public POEqualsDefinition(LexLocation location, POPattern pattern,
		POTypeBind typebind, POBind setbind, POExpression test,
		TCType expType, TCType defType, PODefinitionList defs)
	{
		super(location, null);
		this.pattern = pattern;
		this.typebind = typebind;
		this.bind = setbind;
		this.test = test;
		this.expType = expType;
		this.defType = defType;
		this.defs = defs;
	}

	@Override
	public TCType getType()
	{
		return defType != null ? defType : new TCUnknownType(location);
	}

	@Override
	public String toString()
	{
		return (pattern != null ? pattern :
				typebind != null ? typebind : bind) + " = " + test;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof POEqualsDefinition)
		{
			return toString().equals(other.toString());
		}
		
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList list = new ProofObligationList();

		if (pattern != null)
		{
			if (!(pattern instanceof POIdentifierPattern) &&
				!(pattern instanceof POIgnorePattern) &&
				expType.isUnion(location))
			{
				TCType patternType = pattern.getPossibleType();	// With unknowns
				TCUnionType ut = expType.getUnion();
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

	    			if (!TypeComparator.isSubType(
	    				ctxt.checkType(test, expType), compatible))
	    			{
	    				list.add(new ValueBindingObligation(this, ctxt));
	    				list.add(new SubTypeObligation(test, compatible, expType, ctxt));
	    			}
				}
			}
		}
		else if (typebind != null)
		{
			if (!TypeComparator.isSubType(ctxt.checkType(test, expType), defType))
			{
				list.add(new SubTypeObligation(test, defType, expType, ctxt));
			}
		}
		else if (bind instanceof POSetBind)
		{
			list.addAll(((POSetBind)bind).set.getProofObligations(ctxt));
			list.add(new SetMemberObligation(test, ((POSetBind)bind).set, ctxt));
		}
		else if (bind instanceof POSeqBind)
		{
			list.addAll(((POSeqBind)bind).sequence.getProofObligations(ctxt));
			list.add(new SeqMemberObligation(test, ((POSeqBind)bind).sequence, ctxt));
		}

		list.addAll(test.getProofObligations(ctxt));
		return list;
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseEqualsDefinition(this, arg);
	}
}
