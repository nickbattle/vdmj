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

package com.fujitsu.vdmj.traces;

import java.util.Vector;

import com.fujitsu.vdmj.in.expressions.INApplyExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INNewExpression;
import com.fujitsu.vdmj.in.expressions.INVariableExpression;
import com.fujitsu.vdmj.in.statements.INCallObjectStatement;
import com.fujitsu.vdmj.in.statements.INCallStatement;
import com.fujitsu.vdmj.in.statements.INSkipStatement;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.traces.INTraceVariableStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;

@SuppressWarnings("serial")
public class CallSequence extends Vector<INStatement>
{
	private static final int MAXARGLEN = 50;	// Limits arg expansion

	public String getCallString(Context context)
	{
		StringBuilder sb = new StringBuilder();
		String sep = "";
		Context ctxt = new Context(LexLocation.ANY, "traces", context);
		ctxt.setThreadState(null);

		for (INStatement stmt: this)
		{
    		if (stmt instanceof INTraceVariableStatement)
    		{
    			INTraceVariableStatement tvs = (INTraceVariableStatement)stmt;
    			ctxt.put(tvs.var.name, tvs.var.value);
     		}
    		else if (stmt instanceof INCallStatement)
    		{
    			INCallStatement cs = (INCallStatement)stmt;
       			sb.append(sep);
       			sb.append(opArgs(cs.name.getName(), cs.args, ctxt));
       			sep = "; ";
    		}
    		else if (stmt instanceof INCallObjectStatement)
    		{
    			INCallObjectStatement cos = (INCallObjectStatement)stmt;
       			sb.append(sep);
       			sb.append(cos.designator);
       			sb.append(".");
       			sb.append(opArgs(cos.fieldname.getName(), cos.args, ctxt));
       			sep = "; ";
    		}
    		else if (stmt instanceof INSkipStatement)
    		{
       			sb.append(sep);
    			sb.append("skip");
       			sep = "; ";
    		}
		}

		return sb.toString();
	}
	
	private String opArgs(String name, INExpressionList args, Context ctxt)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("(");
		String comma = "";

		for (INExpression argExp: args)
		{
			sb.append(comma);
			String argStr = null;
			
			if (argExp instanceof INVariableExpression)
			{
				INVariableExpression var = (INVariableExpression)argExp;
				argStr = var.name.getName();	// Remove module/class
			}
			else
			{
				argStr = argExp.toString();
			}
			
			if (argExp instanceof INApplyExpression || argExp instanceof INNewExpression)
			{
				sb.append(argStr);	// Don't even try to execute these things!
			}
			else
			{
				try
				{
					String evalStr = argExp.eval(ctxt).toString();
					
					if (evalStr.length() > MAXARGLEN)
					{
						sb.append(argStr);	// Value is just too long (eg. large maps)
					}
					else
					{
						sb.append(evalStr);
					}
				}
				catch (Exception ex)
				{
					// If the value includes mk_R() expressions, we may not be able to resolve the type,
					// either because the type R is not explicit (like mk_M`R) or because the type may
					// have been exported from its module as a "struct" and so mk_R(x) is not legal.
					// Note that a RecordValue's toString does not explicitly expand the module name.
					// So we exclude anything with mk_R expressions too...

					sb.append(argStr);	// Something failed while trying to evaluate the arg
				}
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

		for (INStatement stmt: this)
		{
    		if (stmt instanceof INTraceVariableStatement)
    		{
    			INTraceVariableStatement tvs = (INTraceVariableStatement)stmt;

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
    		else if (stmt instanceof INCallStatement)
    		{
    			INCallStatement cs = (INCallStatement)stmt;
       			sb.append(sep);
       			sb.append(cs.name.getName());
       			sep = "; ";
     		}
    		else if (stmt instanceof INCallObjectStatement)
    		{
    			INCallObjectStatement cs = (INCallObjectStatement)stmt;
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
		
		if (other.size() < size())
		{
			return false;
		}

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

			if (!(get(i) instanceof INTraceVariableStatement))
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

//	public void typeCheck(Interpreter interpreter, Environment environment) throws Exception
//	{
//		for (INStatement statement: this)
//		{
//			if (statement instanceof INCallStatement)
//			{
//				INCallStatement call = (INCallStatement)statement;
//				
//				if (call.name.getTypeQualifier() != null)
//				{
//					continue;	// Already type checked
//				}
//			}
//			else if (statement instanceof INCallObjectStatement)
//			{
//				INCallObjectStatement call = (INCallObjectStatement)statement;
//				
//				if (call.field != null)
//				{
//					continue;	// Already type checked
//				}
//			}
//			
//			interpreter.typeCheck(statement, environment);
//		}
//	}
}
