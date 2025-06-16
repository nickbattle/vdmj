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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.patterns.INPatternBind;
import com.fujitsu.vdmj.in.patterns.INSeqBind;
import com.fujitsu.vdmj.in.patterns.INSetBind;
import com.fujitsu.vdmj.in.patterns.INTypeBind;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ExitException;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;

public class INTrapStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INPatternBind patternBind;
	public final INStatement with;
	public final INStatement body;

	public INTrapStatement(LexLocation location, INPatternBind patternBind, INStatement with, INStatement body)
	{
		super(location);
		this.patternBind = patternBind;
		this.with = with;
		this.body = body;
	}

	@Override
	public String toString()
	{
		return "trap " + patternBind + " with " + with + " in " + body;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		Value rv = null;

		try
		{
			rv = body.eval(ctxt);
		}
		catch (ExitException e)
		{
			Value exval = e.value;

			try
			{
    			if (patternBind.pattern != null)
    			{
    				if (!exval.isUndefined())	// "exit" throws the undefined value
    				{
	    				Context evalContext = new Context(location, "trap pattern", ctxt);
	    				evalContext.putList(patternBind.pattern.getNamedValues(exval, ctxt));
	    				rv = with.eval(evalContext);
    				}
    				else
    				{
    					throw e;
    				}
    			}
    			else if (patternBind.bind instanceof INSetBind)
    			{
    				INSetBind setbind = (INSetBind)patternBind.bind;
    				ValueSet set = setbind.set.eval(ctxt).setValue(ctxt);

    				if (set.contains(exval))
    				{
    					Context evalContext = new Context(location, "trap set", ctxt);
    					evalContext.putList(setbind.pattern.getNamedValues(exval, ctxt));
    					rv = with.eval(evalContext);
    				}
    				else
    				{
    					throw e;
    				}
    			}
    			else if (patternBind.bind instanceof INSeqBind)
    			{
    				INSeqBind seqbind = (INSeqBind)patternBind.bind;
    				ValueList seq = seqbind.sequence.eval(ctxt).seqValue(ctxt);

    				if (seq.contains(exval))
    				{
    					Context evalContext = new Context(location, "trap seq", ctxt);
    					evalContext.putList(seqbind.pattern.getNamedValues(exval, ctxt));
    					rv = with.eval(evalContext);
    				}
    				else
    				{
    					throw e;
    				}
    			}
    			else
    			{
    				INTypeBind typebind = (INTypeBind)patternBind.bind;
    				Value converted = exval.convertTo(typebind.type, ctxt);
    				Context evalContext = new Context(location, "trap type", ctxt);
    				evalContext.putList(typebind.pattern.getNamedValues(converted, ctxt));
    				rv = with.eval(evalContext);
    			}
			}
			catch (ValueException ve)
			{
				throw e;
			}
			catch (PatternMatchException pe)
			{
				throw e;
			}
		}

		return rv;
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTrapStatement(this, arg);
	}
}
