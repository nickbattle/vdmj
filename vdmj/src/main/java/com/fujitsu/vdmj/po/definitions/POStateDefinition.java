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

import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POMkTypeExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.PONameContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SatisfiabilityObligation;
import com.fujitsu.vdmj.pog.StateInitObligation;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFieldList;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold a module's state definition.
 */
public class POStateDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final TCFieldList fields;
	public final POPattern invPattern;
	public final POExpression invExpression;
	public final POPattern initPattern;
	public final POExpression initExpression;
	public final POExplicitFunctionDefinition invdef;
	public final POExplicitFunctionDefinition initdef;

	public final TCRecordType recordType;

	public POStateDefinition(TCNameToken name, TCFieldList fields,
		POPattern invPattern, POExpression invExpression,
		POPattern initPattern, POExpression initExpression,
		POExplicitFunctionDefinition invdef, POExplicitFunctionDefinition initdef,
		TCRecordType recordType)
	{
		super(name.getLocation(), name);

		this.fields = fields;
		this.invPattern = invPattern;
		this.invExpression = invExpression;
		this.initPattern = initPattern;
		this.initExpression = initExpression;
		this.invdef = invdef;
		this.initdef = initdef;
		this.recordType = recordType;
	}

	@Override
	public String toString()
	{
		return "state " + name + " of\n" + Utils.listToString(fields, "\n") +
				(invPattern == null ? "" : "\n\tinv " + invPattern + " == " + invExpression) +
	    		(initPattern == null ? "" : "\n\tinit " + initPattern + " == " + initExpression) +
	    		"\nend";
	}
	
	@Override
	public String toPattern(boolean maximal)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("mk_");
		sb.append(name);
		if (maximal) sb.append("!");
		sb.append("(");
		String sep = "";

		for (TCField field: fields)
		{
			sb.append(sep);
			sb.append(field.tag);
			sep = ", ";
		}
		
		sb.append(")");
		return sb.toString();
	}

	public POMkTypeExpression getMkExpression()
	{
		POExpressionList args = new POExpressionList();
		TCTypeList argTypes = new TCTypeList();

		for (TCField f: fields)
		{
			args.add(new POVariableExpression(f.tagname, null));
			argTypes.add(f.type);
		}

		return new POMkTypeExpression(name, args, recordType, argTypes);
	}

	@Override
	public TCType getType()
	{
		return recordType;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList list =
				(annotations != null) ? annotations.poBefore(this, ctxt) : new ProofObligationList();

		ctxt.pop();		// Remove default name context
		ctxt.push(new PONameContext(new TCNameList(this.name)));
				
		if (invExpression != null)
		{
			list.addAll(invdef.getProofObligations(ctxt, pogState, env));
			list.add(new SatisfiabilityObligation(this, ctxt));
		}
		
		if (initExpression != null)
		{
			list.addAll(initdef.getProofObligations(ctxt, pogState, env));
			list.add(new StateInitObligation(this, ctxt));
		}

		if (annotations != null) annotations.poAfter(this, list, ctxt);
		return list;
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseStateDefinition(this, arg);
	}

}
