/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package annotations.in;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.definitions.INValueDefinition;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INVariableExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;

public class INWitnessAnnotation extends INAnnotation
{
	private static final long serialVersionUID = 1L;
	private static Context witnessCtxt = null;
	private final INValueDefinition myDefinition;

	public INWitnessAnnotation(TCIdentifierToken name, INExpressionList args, INValueDefinition myDefinition)
	{
		super(name, args);
		this.myDefinition = myDefinition;
	}
	
	public static void doInit()
	{
		Context root = Interpreter.getInstance().getInitialContext();
		witnessCtxt = new Context(LexLocation.ANY, "Witness context", root);
		witnessCtxt.setThreadState(CPUValue.vCPU);
		boolean retry = false;
		int retries = 3;
		List<ContextException> problems = new Vector<ContextException>();
		
		do
		{
			problems.clear();
			
			for (INAnnotation a: getInstances(INWitnessAnnotation.class))
			{
				try
				{
					INWitnessAnnotation witness = (INWitnessAnnotation)a;
					NameValuePairList nvpl = witness.myDefinition.getNamedValues(witnessCtxt);
					witnessCtxt.putList(nvpl);
				}
				catch (ContextException e)
				{
					if (e.number == 4034)	// Name not in scope
					{
						retry = true;
					}
					else
					{
						problems.add(e);	// Store to be reported
					}
				}
			}
		}
		while (retry && --retries > 0);
		
		// Raise a single exception if we have trouble, but list them all first
		if (!problems.isEmpty())
		{
			for (ContextException e: problems)
			{
				Console.err.println(e.toString());
			}
			
			ContextException first = problems.get(0);
			throw new ContextException(6666, "Bad witness(es)", first.location, witnessCtxt);
		}
	}
	
	@Override
	protected void doInit(Context ctxt)
	{
		INVariableExpression ve = (INVariableExpression)args.get(0);
		Value result = witnessCtxt.get(ve.name);
		System.out.println(this + " = " + result);
	}
	
	@Override
	public void inBefore(INStatement stmt, Context ctxt)
	{
	}
}
