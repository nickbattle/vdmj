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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternBind;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCForPatternBindStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCPatternBind patternBind;
	public final boolean reverse;
	public final TCExpression seqexp;
	public final TCStatement statement;
	
	public TCType expType;

	public TCForPatternBindStatement(LexLocation location,
		TCPatternBind patternBind, boolean reverse, TCExpression seqexp, TCStatement body)
	{
		super(location);
		this.patternBind = patternBind;
		this.reverse = reverse;
		this.seqexp = seqexp;
		this.statement = body;
	}

	@Override
	public String toString()
	{
		return "for " + patternBind + " in " +
			(reverse ? " reverse " : "") + seqexp + " do\n" + statement;
	}

	@Override
	public TCType typeCheck(Environment base, NameScope scope, TCType constraint, boolean mandatory)
	{
		expType = seqexp.typeCheck(base, null, scope, null);
		Environment local = base;

		if (expType.isSeq(location))
		{
			TCSeqType st = expType.getSeq();
			patternBind.typeCheck(base, scope, st.seqof);
			TCDefinitionList defs = patternBind.getDefinitions();
			defs.typeCheck(base, scope);
			local = new FlatCheckedEnvironment(defs, base, scope);
			TCType rt = statement.typeCheck(local, scope, constraint, mandatory);
			
			if (!(st instanceof TCSeq1Type) && !(rt instanceof TCVoidType))
			{
				// Union with () because the loop may not be entered
				rt = new TCUnionType(location, rt, new TCVoidType(location));
			}
			
			local.unusedCheck();
			return setType(rt);
		}
		else
		{
			seqexp.report(3223, "Expecting sequence type after 'in'");
			return setType(new TCUnknownType(location));
		}
	}

	/**
	 * Find the TCPattern that defines the loop variable(s).
	 */
	public TCPattern getPattern()
	{
		if (patternBind.pattern != null)
		{
			return patternBind.pattern;
		}
		else if (patternBind.bind instanceof TCTypeBind)
		{
			TCTypeBind tb = (TCTypeBind)patternBind.bind;
			return tb.pattern;
		}
		else if (patternBind.bind instanceof TCSetBind)
		{
			TCSetBind sb = (TCSetBind)patternBind.bind;
			return sb.pattern;
		}
		else // (patternBind.bind instanceof TCSeqBind)
		{
			TCSeqBind sb = (TCSeqBind)patternBind.bind;
			return sb.pattern;
		}
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForPatternBindStatement(this, arg);
	}
}
