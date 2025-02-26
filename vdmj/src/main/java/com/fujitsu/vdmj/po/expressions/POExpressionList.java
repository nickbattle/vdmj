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

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.util.Utils;


@SuppressWarnings("serial")
public class POExpressionList extends POMappedList<TCExpression, POExpression>
{
	public POExpressionList(TCExpressionList from) throws Exception
	{
		super(from);
	}
	
	public POExpressionList()
	{
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}

	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList list = new ProofObligationList();

		for (POExpression e: this)
		{
			list.addAll(e.getProofObligations(ctxt, pogState, env));
		}

		return list;
	}
}
