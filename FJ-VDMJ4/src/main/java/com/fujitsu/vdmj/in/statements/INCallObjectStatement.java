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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INCallObjectStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INObjectDesignator designator;
	public final TCNameToken classname;
	public final TCIdentifierToken fieldname;
	public final INExpressionList args;
	public final boolean explicit;
	public TCNameToken field;

	public INCallObjectStatement(INObjectDesignator designator, TCNameToken classname,
		TCIdentifierToken fieldname, INExpressionList args, TCNameToken field)
	{
		super(designator.location);

		this.designator = designator;
		this.classname = classname;
		this.fieldname = fieldname;
		this.args = args;
		this.explicit = classname != null && classname.isExplicit();
		this.field = field;
	}

	@Override
	public String toString()
	{
		return designator + "." +
			(classname != null ? classname : fieldname) +
			"(" + Utils.listToString(args) + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		location.hit();

		// The check above increments the hit counter for the call, but so
		// do the evaluations of the designator below, so we correct the
		// hit count here...

		location.hits--;
		
		boolean endstop = breakpoint.catchReturn(ctxt);

		try
		{
			ValueList argValues = new ValueList();

			for (INExpression arg: args)
			{
				argValues.add(arg.eval(ctxt));
			}
			
			// Work out the actual types of the arguments, so we bind the right op/fn
			TCTypeList argTypes = new TCTypeList();
			int arg = 0;
			
			for (TCType argType: field.getTypeQualifier())
			{
				if (argType instanceof TCUnionType)
				{
					TCUnionType u = (TCUnionType)argType;
					
					for (TCType possible: u.types)
					{
						try
						{
							argValues.get(arg).convertTo(possible, ctxt);
							argTypes.add(possible);
							break;
						}
						catch (ValueException e)
						{
							// Try again
						}
					}
				}
				else
				{
					argTypes.add(argType);
				}
				
				arg++;
			}
			
			if (argTypes.size() != field.getTypeQualifier().size())
			{
				ExceptionHandler.abort(location, 4168, "Arguments do not match parameters: " + field, ctxt);
			}
			else
			{
				field = field.getModifiedName(argTypes);
			}

			ObjectValue obj = designator.eval(ctxt).objectValue(ctxt);
			Value v = obj.get(field, explicit);

			if (v == null)
			{
    			ExceptionHandler.abort(location, 4035, "Object has no field: " + field.getName(), ctxt);
			}

			v = v.deref();

			if (v instanceof OperationValue)
			{
    			OperationValue op = v.operationValue(ctxt);
    			Value rv = op.eval(location, argValues, ctxt);

    			if (endstop && !breakpoint.isContinue(ctxt))
           		{
           			breakpoint.enterDebugger(ctxt);
           		}
           		
    			return rv;
			}
			else
			{
    			FunctionValue fn = v.functionValue(ctxt);
    			Value rv = fn.eval(location, argValues, ctxt);

    			if (endstop && !breakpoint.isContinue(ctxt))
           		{
           			breakpoint.enterDebugger(ctxt);
           		}
           		
    			return rv;
			}
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		return args.findExpression(lineno);
	}
}
