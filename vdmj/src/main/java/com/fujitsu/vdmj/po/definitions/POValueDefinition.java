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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.patterns.POIgnorePattern;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.pog.ValueBindingObligation;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.visitors.TCExplicitTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
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
	public final PODefinitionList defs;

	private POExpression extracted = null;	// With removed operations

	public POValueDefinition(POAnnotationList annotations, POPattern p, TCType type, POExpression exp, TCType expType, PODefinitionList defs)
	{
		super(p.location, null);
		
		this.annotations = annotations;
		this.pattern = p;
		this.type = type;
		this.exp = exp;
		this.expType = expType;
		this.defs = defs;
	}

	@Override
	public String toString()
	{
		return toExplicitString(location);
	}
	
	@Override
	public String toExplicitString(LexLocation from)
	{
		return pattern + (type == null ? "" : " : " + type.apply(new TCExplicitTypeVisitor(), from.module)) +
			 " = " + (extracted != null ? extracted : exp);
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
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList list =
				(annotations != null) ? annotations.poBefore(this, ctxt) : new ProofObligationList();

		this.extracted = POStatement.extractOpCalls(exp, list, pogState, ctxt, env);
		list.addAll(extracted.getProofObligations(ctxt, pogState, env));

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
    				list.addAll(SubTypeObligation.getAllPOs(extracted, compatible, type, ctxt));
    			}
			}
		}

		if (!TypeComparator.isSubType(ctxt.checkType(exp, expType), type))
		{
			list.addAll(SubTypeObligation.getAllPOs(extracted, type, expType, ctxt));
		}
		
		if (annotations != null) annotations.poAfter(this, list, ctxt);
		return list;
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseValueDefinition(this, arg);
	}
}
