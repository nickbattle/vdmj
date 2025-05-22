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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;

public class TheoremObligation extends ProofObligation
{
	private final POVariableExpression theoremName;
	
	public TheoremObligation(LexLocation location, POVariableExpression theoremName, POExpression theorem, POContextStack ctxt)
	{
		super(location, POType.THEOREM, ctxt);
		this.theoremName = theoremName;
		this.source = theorem.toString();
		setObligationVars(ctxt, theorem);
		setReasonsAbout(ctxt.getReasonsAbout());
	}
	
	public POVariableExpression getTheoremName()
	{
		return theoremName;
	}
}
