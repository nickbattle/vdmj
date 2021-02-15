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

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class POMuExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression record;
	public final PORecordModifierList modifiers;
	public final TCRecordType recordType;
	public final TCTypeList modTypes;

	public POMuExpression(LexLocation location,
		POExpression record, PORecordModifierList modifiers, TCRecordType recordType, TCTypeList modTypes)
	{
		super(location);
		this.record = record;
		this.modifiers = modifiers;
		this.recordType = recordType;
		this.modTypes = modTypes;
	}

	@Override
	public String toString()
	{
		return "mu(" + record + ", " + Utils.listToString(modifiers) + ")";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList list = record.getProofObligations(ctxt);
		int i = 0;

		for (PORecordModifier rm: modifiers)
		{
			list.addAll(rm.value.getProofObligations(ctxt));

			TCField f = recordType.findField(rm.tag.getName());
			TCType mtype = modTypes.get(i++);

			if (f != null)
			{
				if (!TypeComparator.isSubType(mtype, f.type))
				{
					list.add(new SubTypeObligation(rm.value, f.type, mtype, ctxt));
				}
			}
		}

		return list;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMuExpression(this, arg);
	}
}
