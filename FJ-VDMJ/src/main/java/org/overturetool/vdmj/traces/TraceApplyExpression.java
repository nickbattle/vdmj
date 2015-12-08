/*******************************************************************************
 *
 *	Copyright (C) 2008 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.traces;

import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.statements.Statement;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;

/**
 * A class representing a trace apply expression.
 */

public class TraceApplyExpression extends TraceCoreDefinition
{
    private static final long serialVersionUID = 1L;
	public final Statement callStatement;

	public TraceApplyExpression(Statement stmt)
	{
		super(stmt.location);
		this.callStatement = stmt;
	}

	@Override
	public String toString()
	{
		return callStatement.toString();
	}

	@Override
	public void typeCheck(Environment env, NameScope scope)
	{
		callStatement.typeCheck(env, scope, null);
	}

	@Override
	public TraceIterator getIterator(Context ctxt)
	{
		return new StatementIterator(callStatement);
	}
}
