/*******************************************************************************
 *
 *	Copyright (C) 2008, 2009 Fujitsu Services Ltd.
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

import java.util.Vector;

import org.overturetool.vdmj.expressions.ApplyExpression;
import org.overturetool.vdmj.expressions.Expression;
import org.overturetool.vdmj.expressions.ExpressionList;
import org.overturetool.vdmj.expressions.NewExpression;
import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.statements.CallObjectStatement;
import org.overturetool.vdmj.statements.CallStatement;
import org.overturetool.vdmj.statements.Statement;

@SuppressWarnings("serial")
public class CallSequence extends Vector<Statement>
{
	public String getCallString(Context context)
	{
		StringBuilder sb = new StringBuilder();
		String sep = "";
		Context ctxt = new Context(new LexLocation(), "traces", context);
		ctxt.setThreadState(null, null);

		for (Statement stmt: this)
		{
    		if (stmt instanceof TraceVariableStatement)
    		{
    			TraceVariableStatement tvs = (TraceVariableStatement)stmt;
    			ctxt.put(tvs.var.name, tvs.var.value);
     		}
    		else if (stmt instanceof CallStatement)
    		{
    			CallStatement cs = (CallStatement)stmt;
       			sb.append(sep);
       			sb.append(opArgs(cs.name.name, cs.args, ctxt));
       			sep = "; ";
    		}
    		else if (stmt instanceof CallObjectStatement)
    		{
    			CallObjectStatement cos = (CallObjectStatement)stmt;
       			sb.append(sep);
       			sb.append(cos.designator);
       			sb.append(".");
       			sb.append(opArgs(cos.fieldname.name, cos.args, ctxt));
       			sep = "; ";
    		}
		}

		return sb.toString();
	}
	
	private String opArgs(String name, ExpressionList args, Context ctxt)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("(");
		String comma = "";

		for (Expression e: args)
		{
			sb.append(comma);
			
			if (e instanceof ApplyExpression ||
				e instanceof NewExpression)
			{
				sb.append(e.toString());	// Don't actually execute these things!
			}
			else
			{
				sb.append(e.eval(ctxt));
			}
			
			comma = ", ";
		}

		sb.append(")");
		return sb.toString();
	}

	public String toShape(TraceReductionType type)
	{
		StringBuilder sb = new StringBuilder();
		String sep = "";

		for (Statement stmt: this)
		{
    		if (stmt instanceof TraceVariableStatement)
    		{
    			TraceVariableStatement tvs = (TraceVariableStatement)stmt;

       			switch (type)
    			{
       				case SHAPES_NOVARS:
       					break;

       				case SHAPES_VARNAMES:
       	       			sb.append(sep);
       					sb.append(tvs.var.name);
       	       			sep = "; ";
       					break;

       				case SHAPES_VARVALUES:
       	       			sb.append(sep);
       					sb.append(tvs.toString());
       	       			sep = "; ";
       					break;
       					
					case NONE:
					case RANDOM:
						break;
    			}
     		}
    		else if (stmt instanceof CallStatement)
    		{
    			CallStatement cs = (CallStatement)stmt;
       			sb.append(sep);
       			sb.append(cs.name.name);
       			sep = "; ";
     		}
    		else if (stmt instanceof CallObjectStatement)
    		{
    			CallObjectStatement cs = (CallObjectStatement)stmt;
       			sb.append(sep);
       			sb.append(cs.fieldname);
       			sep = "; ";
    		}
		}

		return sb.toString();
	}

	public boolean compareStem(CallSequence other, int upto)
	{
		// Note that the upto count does not include the variable statements
		// that may be in the sequences, but those variables do need to be
		// included in the stem match. "count" is the position ignoring any
		// variable statements.

		int i = 0;
		
		for (int count=0; count<upto;)
		{
			if (i >= size())
			{
				return false;
			}

			if (!compareItem(other, i))
			{
				return false;
			}

			if (!(get(i) instanceof TraceVariableStatement))
			{
				count++;	// Only increment for non-variable statements
			}

			i++;
		}

		return true;
	}

	private boolean compareItem(CallSequence other, int i)
	{
		return get(i).toString().equals(other.get(i).toString());
	}

//	public void typeCheck(Interpreter interpreter, Environment env) throws Exception
//	{
//		for (Statement statement: this)
//		{
//			if (statement instanceof CallStatement)
//			{
//				CallStatement call = (CallStatement)statement;
//				
//				if (call.name.typeQualifier != null)
//				{
//					continue;	// Already type checked
//				}
//			}
//			else if (statement instanceof CallObjectStatement)
//			{
//				CallObjectStatement call = (CallObjectStatement)statement;
//				
//				if (call.field != null)
//				{
//					continue;	// Already type checked
//				}
//			}
//			
//			interpreter.typeCheck(statement, env);
//		}
//	}
}
