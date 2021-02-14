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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.values.NameValuePairList;

public class INThreadDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final INStatement statement;
	public final TCNameToken operationName;
	public final INExplicitOperationDefinition operationDef;

	public INThreadDefinition(INStatement statement, INExplicitOperationDefinition operationDef)
	{
		super(statement.location, new INAccessSpecifier(false, false, Token.PROTECTED, false), null);

		this.statement = statement;
		this.operationName = TCNameToken.getThreadName(statement.location);
		this.operationDef = operationDef;
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
		return operationDef.getNamedValues(ctxt);
	}

	@Override
	public boolean isOperation()
	{
		return true;	// Because we define an operation called "thread"
	}

	@Override
	public TCType getType()
	{
		return new TCUnknownType(location);
	}

	@Override
	public String toString()
	{
		return "thread " + statement.toString();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof INThreadDefinition)
		{
			INThreadDefinition tho = (INThreadDefinition)other;
			return tho.operationName.equals(operationName);
		}
		
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return operationName.hashCode();
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseThreadDefinition(this, arg);
	}
}
