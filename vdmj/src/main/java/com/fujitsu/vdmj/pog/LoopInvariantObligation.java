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

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.expressions.POExpression;

public class LoopInvariantObligation extends ProofObligation
{
	private LoopInvariantObligation(LexLocation location, POContextStack ctxt, POExpression invariant)
	{
		super(location, POType.LOOP_INVARIANT, ctxt);
		source = ctxt.getSource(invariant.toString());
	}
	
	public LoopInvariantObligation(LexLocation location, POContextStack ctxt)
	{
		super(location, POType.LOOP_INVARIANT, ctxt);
		source = ctxt.getSource("-- Missing @LoopInvariant");
		this.markUnchecked("Missing @LoopInvariant");
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static ProofObligationList getAllPOs(LexLocation location, POContextStack ctxt, POExpression invariant)
	{
		ProofObligationList results = new ProofObligationList();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new LoopInvariantObligation(location, choice, invariant));
		}
		
		return results;
	}
}
