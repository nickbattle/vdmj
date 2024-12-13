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

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POSeqBind;
import com.fujitsu.vdmj.po.patterns.POSetBind;
import com.fujitsu.vdmj.po.patterns.POTypeBind;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class POForAllSequenceContext extends POContext
{
	public final String pattern;
	public final String exp;
	private final TCNameSet reasonsAbout;
	
	private String seqset = " in seq ";

	public POForAllSequenceContext(POSetBind bind, POExpression exp)
	{
		this.pattern = bind.pattern.toString();
		this.exp = exp.toString();
		this.reasonsAbout = bind.getVariableNames();
		this.reasonsAbout.addAll(exp.getVariableNames());
	}

	public POForAllSequenceContext(POSeqBind bind, POExpression exp)
	{
		this.pattern = bind.pattern.toString();
		this.exp = exp.toString();
		this.reasonsAbout = bind.getVariableNames();
		this.reasonsAbout.addAll(exp.getVariableNames());
	}

	public POForAllSequenceContext(POTypeBind bind, POExpression exp)
	{
		this.pattern = bind.pattern.toString();
		this.exp = exp.toString();
		this.reasonsAbout = bind.getVariableNames();
		this.reasonsAbout.addAll(exp.getVariableNames());
	}

	public POForAllSequenceContext(POPattern pattern, POExpression exp)
	{
		this.pattern = pattern.toString();
		this.exp = exp.toString();
		this.reasonsAbout = exp.getVariableNames();
		this.reasonsAbout.addAll(pattern.getVariableNames());
	}

	public POForAllSequenceContext(TCNameToken var, POExpression from, POExpression to, POExpression by)
	{
		this.pattern = var.getName();
		
		if (by != null)
		{
			this.exp = String.format("[ %1$s + $var * %3$s | $var in set {0, ..., (%2$s - %1$s) / %3$s} ]", from, to, by);
		}
		else
		{
			this.exp = String.format("[ $var | $var in set {%1$s, ..., %2$s} ]", from, to);
		}
		
		this.reasonsAbout = from.getVariableNames();
		this.reasonsAbout.addAll(to.getVariableNames());
		if (by != null) this.reasonsAbout.addAll(by.getVariableNames());
		this.reasonsAbout.add(var);

	}

	public POForAllSequenceContext(POPattern pattern, POExpression set, String seqset)
	{
		this.pattern = pattern.toString();
		this.exp = set.toString();
		this.seqset = seqset;
		this.reasonsAbout = set.getVariableNames();
		this.reasonsAbout.addAll(pattern.getVariableNames());
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("forall ");
		sb.append(pattern);
		sb.append(seqset);
		sb.append(exp);
		sb.append(" & ");

		return sb.toString();
	}
	
	@Override
	public TCNameSet reasonsAbout()
	{
		return reasonsAbout;
	}
}
