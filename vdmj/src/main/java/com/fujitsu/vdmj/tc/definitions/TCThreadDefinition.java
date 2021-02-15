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

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;

public class TCThreadDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCStatement statement;
	private TCNameToken operationName = null;
	public TCExplicitOperationDefinition operationDef = null;

	public TCThreadDefinition(TCStatement statement)
	{
		super(Pass.DEFS, statement.location, null, NameScope.GLOBAL);

		this.statement = statement;
		this.operationName = TCNameToken.getThreadName(statement.location);

		setAccessSpecifier(new TCAccessSpecifier(false, false, Token.PUBLIC, false));
	}

	@Override
	public void implicitDefinitions(Environment base)
	{
		operationDef = getThreadDefinition();
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		return operationDef.findName(sought, scope);
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return new TCDefinitionList(operationDef);
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
	public String kind()
	{
		return "thread";
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof TCThreadDefinition)
		{
			TCThreadDefinition tho = (TCThreadDefinition)other;
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
	public void typeCheck(Environment base, NameScope scope)
	{
		FlatEnvironment local = new FlatEnvironment(getSelfDefinition(), base);
		TCType rt = statement.typeCheck(local, NameScope.NAMESANDSTATE, null, false);

		if (!(rt instanceof TCVoidType) && !(rt instanceof TCUnknownType))
		{
			report(3049, "Thread statement/operation must not return a value");
		}
	}

	private TCExplicitOperationDefinition getThreadDefinition()
	{
		TCOperationType type = new TCOperationType(location);	// () ==> ()

		TCExplicitOperationDefinition def = new TCExplicitOperationDefinition(null, TCAccessSpecifier.DEFAULT,
			operationName, type, new TCPatternList(), null, null, statement);

		def.setAccessSpecifier(accessSpecifier);
		def.classDefinition = classDefinition;
		return def;
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseThreadDefinition(this, arg);
	}
}
