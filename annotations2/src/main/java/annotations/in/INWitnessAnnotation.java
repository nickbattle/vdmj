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

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.definitions.INValueDefinition;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;

public class INWitnessAnnotation extends INAnnotation
{
	private static final long serialVersionUID = 1L;
	private static NameValuePairList tagValues = null;
	private final INValueDefinition myDefinition;

	public INWitnessAnnotation(TCIdentifierToken name, INExpressionList args, INValueDefinition myDefinition)
	{
		super(name, args);
		this.myDefinition = myDefinition;
	}
	
	public static void doInit()
	{
		tagValues  = new NameValuePairList();
		Context ctxt = new Context(LexLocation.ANY, "Witness context", null);
		ctxt.setThreadState(CPUValue.vCPU);
		
		for (INAnnotation a: getInstances(INWitnessAnnotation.class))
		{
			INWitnessAnnotation witness = (INWitnessAnnotation)a;
			NameValuePairList nvpl = witness.myDefinition.getNamedValues(ctxt);
			ctxt.putList(nvpl);
			tagValues.addAll(nvpl);
		}
	}
	
	@Override
	protected void doInit(Context ctxt)
	{
		Context local = new Context(LexLocation.ANY, "Witness context", ctxt);
		ctxt.setThreadState(CPUValue.vCPU);
		ctxt.putList(tagValues);
		
		// Evaluate things using "local" context
		Value result = args.get(1).eval(local);
		System.out.println(this + " = " + result);
	}
	
	@Override
	public void inBefore(INStatement stmt, Context ctxt)
	{
	}
}
