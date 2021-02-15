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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.patterns.INPatternBind;
import com.fujitsu.vdmj.in.patterns.INSeqBind;
import com.fujitsu.vdmj.in.patterns.INSetBind;
import com.fujitsu.vdmj.in.patterns.INTypeBind;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;

public class INTixeStmtAlternative
{
	public final INPatternBind patternBind;
	public final INStatement statement;

	public INTixeStmtAlternative(INPatternBind patternBind, INStatement stmt)
	{
		this.patternBind = patternBind;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return patternBind + " |-> " + statement;
	}

	public Value eval(LexLocation location, Value exval, Context ctxt)
	{
		Context evalContext = null;

		try
		{
			if (patternBind.pattern != null)
			{
				evalContext = new Context(location, "tixe pattern", ctxt);
				evalContext.putList(patternBind.pattern.getNamedValues(exval, ctxt));
			}
			else if (patternBind.bind instanceof INSetBind)
			{
				INSetBind setbind = (INSetBind)patternBind.bind;
				ValueSet set = setbind.set.eval(ctxt).setValue(ctxt);

				if (set.contains(exval))
				{
					evalContext = new Context(location, "tixe set", ctxt);
					evalContext.putList(setbind.pattern.getNamedValues(exval, ctxt));
				}
				else
				{
					evalContext = null;
				}
			}
			else if (patternBind.bind instanceof INSeqBind)
			{
				INSeqBind seqbind = (INSeqBind)patternBind.bind;
				ValueList seq = seqbind.sequence.eval(ctxt).seqValue(ctxt);

				if (seq.contains(exval))
				{
					evalContext = new Context(location, "tixe seq", ctxt);
					evalContext.putList(seqbind.pattern.getNamedValues(exval, ctxt));
				}
				else
				{
					evalContext = null;
				}
			}
			else
			{
				INTypeBind typebind = (INTypeBind)patternBind.bind;
				// Note we always perform DTC checks here...
				Value converted = exval.convertValueTo(typebind.type, ctxt);
				evalContext = new Context(location, "tixe type", ctxt);
				evalContext.putList(typebind.pattern.getNamedValues(converted, ctxt));
			}
		}
		catch (ValueException ve)	// INType bind convert failure
		{
			evalContext = null;
		}
		catch (PatternMatchException e)
		{
			evalContext = null;
		}

		return evalContext == null ? null : statement.eval(evalContext);
	}
}
