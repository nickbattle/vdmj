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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.po.expressions.POIotaExpression;

public class UniqueExistenceObligation extends ProofObligation
{
	private UniqueExistenceObligation(POIotaExpression poIotaExpression, POContextStack ctxt)
	{
		super(poIotaExpression.location, POType.UNIQUE_EXISTENCE, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append("exists1 ");
		sb.append(poIotaExpression.bind);
		sb.append(" & ");
		sb.append(poIotaExpression.predicate);

		source = ctxt.getSource(sb.toString());
		setObligationVars(ctxt, poIotaExpression);
		setReasonsAbout(ctxt.getReasonsAbout());
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static List<ProofObligation> getAllPOs(POIotaExpression poIotaExpression, POContextStack ctxt)
	{
		Vector<ProofObligation> results = new Vector<ProofObligation>();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new UniqueExistenceObligation(poIotaExpression, choice));
		}
		
		return results;
	}
}
