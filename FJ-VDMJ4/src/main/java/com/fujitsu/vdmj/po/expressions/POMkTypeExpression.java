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

package com.fujitsu.vdmj.po.expressions;

import java.util.Iterator;

import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class POMkTypeExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken typename;
	public final POExpressionList args;
	public final TCRecordType recordType;
	public final TCTypeList argTypes;

	public POMkTypeExpression(TCNameToken typename, POExpressionList args,
		TCRecordType recordType, TCTypeList argTypes)
	{
		super(typename.getLocation());
		this.typename = typename;
		this.args = args;
		this.recordType = recordType;
		this.argTypes = argTypes;
	}

	@Override
	public String toString()
	{
		return "mk_" + typename + "(" + Utils.listToString(args) + ")";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList list = args.getProofObligations(ctxt);
		Iterator<TCType> it = argTypes.iterator();
		int i = 0;

		for (TCField f: recordType.fields)
		{
			TCType atype = it.next();

			if (!TypeComparator.isSubType(
				ctxt.checkType(args.get(i), atype), f.type))
			{
				list.add(new SubTypeObligation(args.get(i), f.type, atype, ctxt));
			}

			i++;
		}

		if (recordType.invdef != null)
		{
			list.add(new SubTypeObligation(this, recordType, recordType, ctxt));
		}

		return list;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMkTypeExpression(this, arg);
	}
}
