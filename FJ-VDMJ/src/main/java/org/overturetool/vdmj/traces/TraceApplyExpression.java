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

import org.overturetool.vdmj.Settings;
import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.expressions.ExpressionList;
import org.overturetool.vdmj.expressions.MapEnumExpression;
import org.overturetool.vdmj.expressions.MapletExpression;
import org.overturetool.vdmj.expressions.MkTypeExpression;
import org.overturetool.vdmj.expressions.SeqEnumExpression;
import org.overturetool.vdmj.expressions.SetEnumExpression;
import org.overturetool.vdmj.lex.LexException;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.lex.LexTokenReader;
import org.overturetool.vdmj.runtime.Breakpoint;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.statements.CallObjectStatement;
import org.overturetool.vdmj.statements.CallStatement;
import org.overturetool.vdmj.statements.Statement;
import org.overturetool.vdmj.syntax.ExpressionReader;
import org.overturetool.vdmj.syntax.ParserException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.values.ObjectValue;
import org.overturetool.vdmj.values.Value;

/**
 * A class representing a trace apply expression.
 */

public class TraceApplyExpression extends TraceCoreDefinition
{
    private static final long serialVersionUID = 1L;
	public final Statement callStatement;
	public final String currentModule;

	private static final LexLocation NOWHERE = new LexLocation();
	private static final Breakpoint DUMMY = new Breakpoint(NOWHERE);
	
	private Statement lastStatement = null;

	public TraceApplyExpression(Statement stmt, String currentModule)
	{
		super(stmt.location);
		this.callStatement = stmt;
		this.currentModule = currentModule;
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
		ExpressionList newargs = new ExpressionList();
		ExpressionList args = null;

		if (callStatement instanceof CallStatement)
		{
			CallStatement stmt = (CallStatement)callStatement;
			args = stmt.args;
		}
		else
		{
			CallObjectStatement stmt = (CallObjectStatement)callStatement;
			args = stmt.args;
		}

		for (Expression arg: args)
		{
			Value v = arg.eval(ctxt).deref();

			if (v instanceof ObjectValue)
			{
				newargs.add(arg);
			}
			else
			{
    			String value = v.toString();
    			LexTokenReader ltr = new LexTokenReader(value, Settings.dialect);
    			ExpressionReader er = new ExpressionReader(ltr);
    			er.setCurrentModule(currentModule);

    			try
    			{
    				newargs.add(er.readExpression());
    			}
    			catch (ParserException e)
    			{
    				newargs.add(arg);		// Give up!
    			}
    			catch (LexException e)
    			{
    				newargs.add(arg);		// Give up!
    			}
			}
		}
		
		for (Expression arg: newargs)
		{
			reduceExpression(arg);	// Reduce heap usage
		}

		Statement newStatement = null;

		if (callStatement instanceof CallStatement)
		{
			CallStatement stmt = (CallStatement)callStatement;
			newStatement = new CallStatement(stmt.name, newargs);
		}
		else
		{
			CallObjectStatement stmt = (CallObjectStatement)callStatement;
			
			if (stmt.classname != null)
			{
				newStatement = new CallObjectStatement(
					stmt.designator, stmt.classname, newargs);
			}
			else
			{
				newStatement = new CallObjectStatement(
					stmt.designator, stmt.fieldname, newargs);
			}
		}
		
		// If we're generating the same statement, re-use the object to reduce heap
		if (lastStatement != null && lastStatement.toString().equals(newStatement.toString()))
		{
			newStatement = lastStatement;
		}
		
		lastStatement = newStatement;

		return new StatementIterator(newStatement);
	}

	/**
	 * Remove as much as possible from an Expression tree, to reduce heap usage.
	 * For now, just replace the Breakpoints and recurse for larger aggregates.
	 */
	private void reduceExpression(Expression arg)
	{
		arg.breakpoint = DUMMY;
		
		if (arg instanceof SeqEnumExpression)
		{
			SeqEnumExpression s = (SeqEnumExpression)arg;
			reduceExpressions(s.members);
		}
		else if (arg instanceof SetEnumExpression)
		{
			SetEnumExpression s = (SetEnumExpression)arg;
			reduceExpressions(s.members);
		}
		else if (arg instanceof MkTypeExpression)
		{
			MkTypeExpression m = (MkTypeExpression)arg;
			reduceExpressions(m.args);
		}
		else if (arg instanceof MapEnumExpression)
		{
			MapEnumExpression m = (MapEnumExpression)arg;
			
			for (MapletExpression e: m.members)
			{
				reduceExpression(e.left);
				reduceExpression(e.right);
			}
		}
	}
	
	private void reduceExpressions(ExpressionList list)
	{
		for (Expression e: list)
		{
			reduceExpression(e);
		}
	}
}
