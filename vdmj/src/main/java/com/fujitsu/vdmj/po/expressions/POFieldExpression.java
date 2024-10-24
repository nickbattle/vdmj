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

import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;
import com.fujitsu.vdmj.typechecker.Environment;

public class POFieldExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression object;
	public final TCIdentifierToken field;
	public final TCNameToken memberName;

	public POFieldExpression(POExpression object, TCIdentifierToken field, TCNameToken memberName)
	{
		super(object);
		this.object = object;
		this.field = field;
		this.memberName = memberName;
	}

	@Override
	public String toString()
	{
		return "(" + object + "." +
			(memberName == null ? field.getName() : memberName.getName()) + ")";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = object.getProofObligations(ctxt, pogState, env);

		// If the object base is a union of records, we create a subtype PO to say that
		// the object is one of the records that defines this field.
		
		TCTypeQualifier qualifier = new TCTypeQualifier()
		{
			@Override
			public boolean matches(TCType member)
			{
				if (member instanceof TCRecordType)
				{
					TCRecordType rt = (TCRecordType)member;
					return (rt.findField(field.getName()) != null);
				}
				else
				{
					return false;
				}
			}
		};
		
		obligations.addAll(checkUnionQualifiers(object, qualifier, ctxt));

		return obligations;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseFieldExpression(this, arg);
	}
}
