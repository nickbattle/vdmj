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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCSelfExpression;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCVoidReturnType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCReturnStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCExpression expression;

	public TCReturnStatement(LexLocation location, TCExpression expression)
	{
		super(location);
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return "return" + (expression == null ? "" : " (" + expression + ")");
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		TCDefinition enclosing = env.getEnclosingDefinition();
		boolean inConstructor = false;
		
		if (enclosing instanceof TCExplicitOperationDefinition)
		{
			TCExplicitOperationDefinition eod = (TCExplicitOperationDefinition)enclosing;
			inConstructor = eod.isConstructor;
		}
		else if (enclosing instanceof TCImplicitOperationDefinition)
		{
			TCImplicitOperationDefinition iod = (TCImplicitOperationDefinition)enclosing;
			inConstructor = iod.isConstructor;
		}
		
		if (inConstructor && !(expression instanceof TCSelfExpression))
		{
			report(3326, "Constructor can only return 'self'");
		}
		
		if (expression == null)
		{
			return checkReturnType(constraint, new TCVoidReturnType(location), true);
		}
		else if (constraint instanceof TCVoidType)	// Shouldn't have a returned value for ()
		{
			report(3365, "Void operation cannot use 'return <exp>'");
			return new TCUnknownType(location);
		}
		else
		{
			return checkReturnType(constraint, expression.typeCheck(env, null, scope, null), true);
		}
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseReturnStatement(this, arg);
	}
}
