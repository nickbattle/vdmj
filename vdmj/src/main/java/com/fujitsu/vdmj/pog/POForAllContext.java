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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.po.expressions.POExists1Expression;
import com.fujitsu.vdmj.po.expressions.POExistsExpression;
import com.fujitsu.vdmj.po.expressions.POForAllExpression;
import com.fujitsu.vdmj.po.expressions.POIotaExpression;
import com.fujitsu.vdmj.po.expressions.POLambdaExpression;
import com.fujitsu.vdmj.po.expressions.POLetBeStExpression;
import com.fujitsu.vdmj.po.expressions.POMapCompExpression;
import com.fujitsu.vdmj.po.expressions.POSeqCompExpression;
import com.fujitsu.vdmj.po.expressions.POSetCompExpression;
import com.fujitsu.vdmj.po.patterns.POMultipleBind;
import com.fujitsu.vdmj.po.patterns.POMultipleTypeBind;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.POTypeBind;
import com.fujitsu.vdmj.po.statements.POLetBeStStatement;

public class POForAllContext extends POContext
{
	public final List<POMultipleBind> bindings;

	public POForAllContext(POMapCompExpression exp)
	{
		this.bindings = exp.bindings;
	}

	public POForAllContext(POSetCompExpression exp)
	{
		this.bindings = exp.bindings;
	}

	public POForAllContext(POSeqCompExpression exp)
	{
		this.bindings = exp.bind.getMultipleBindList();
	}

	public POForAllContext(POForAllExpression exp)
	{
		this.bindings = exp.bindList;
	}

	public POForAllContext(POExistsExpression exp)
	{
		this.bindings = exp.bindList;
	}

	public POForAllContext(POExists1Expression exp)
	{
		this.bindings = exp.bind.getMultipleBindList();
	}

	public POForAllContext(POIotaExpression exp)
	{
		this.bindings = exp.bind.getMultipleBindList();
	}

	public POForAllContext(POLambdaExpression exp)
	{
		this.bindings = new Vector<POMultipleBind>();

		for (POTypeBind tb: exp.bindList)
		{
			POPatternList pl = new POPatternList();
			pl.add(tb.pattern);
			POMultipleTypeBind mtb = new POMultipleTypeBind(pl, tb.type);
			bindings.add(mtb);
		}
	}

	public POForAllContext(POLetBeStExpression exp)
	{
		this.bindings = exp.bind.getMultipleBindList();
	}

	public POForAllContext(POLetBeStStatement stmt)
	{
		this.bindings = stmt.bind.getMultipleBindList();
	}

	@Override
	public boolean isScopeBoundary()
	{
		return true;
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("forall ");
		String prefix = "";

		for (POMultipleBind mb: bindings)
		{
			sb.append(prefix);
			sb.append(mb);
			prefix = ", ";
		}

		sb.append(" &");

		return sb.toString();
	}
}
