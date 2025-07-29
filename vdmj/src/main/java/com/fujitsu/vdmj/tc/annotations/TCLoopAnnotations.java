/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package com.fujitsu.vdmj.tc.annotations;

import java.util.List;

import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCLoopAnnotations implements Mappable
{
	private final TCLoopInvariantList invariants;
	@SuppressWarnings("unused")
	private final TCLoopMeasureAnnotation measure;

	public TCLoopAnnotations(TCLoopInvariantList invariants, TCLoopMeasureAnnotation measure)
	{
		this.invariants = invariants;
		this.measure = measure;
	}

	public static TCLoopAnnotations getLoopAnnotations(TCStatement stmt)
	{
		List<TCLoopMeasureAnnotation> measures =
			stmt.getAnnotations().getInstances(TCLoopMeasureAnnotation.class);

		if (measures.size() > 1)
		{
			stmt.report(6007, "Only one @LoopMeasure allowed");
		}

		return new TCLoopAnnotations(new TCLoopInvariantList(stmt),
			measures.isEmpty() ? null : measures.get(0));
	}

	/**
	 * Some checks have to be performed across all of the @LoopInvariants declared. So these methods are
	 * called from the typeCheck of each loop statement type.
	 */
	public void typeCheck(Environment env, TCStatement stmt)
	{
		typeCheck(env, stmt, new TCNameList());
	}

	public void typeCheck(Environment env, TCStatement stmt, TCNameList loopVars)
	{
		if (!invariants.getGhostNames().isEmpty())
		{
			if (invariants.isEmpty())
			{
				stmt.report(6007, "@LoopGhost must also have @LoopInvariant(s)");
			}

			if (invariants.getGhostNames().size() > 1)
			{
				stmt.report(6007, "Only one @LoopGhost allowed");
			}
		}

		// Called, even if invariants empty, to set ghost definitions.
		Environment local = invariants.getGhostEnvironment(stmt, env);

		if (!invariants.isEmpty())
		{
			TCNameSet updates = stmt.updatesState(false);
			TCNameSet reasonsAbout = new TCNameSet();
			
			for (TCLoopInvariantAnnotation inv: invariants)
			{
				TCExpression exp = inv.args.firstElement();
				reasonsAbout.addAll(exp.getVariableNames());

				TCType itype = exp.typeCheck(local, null, NameScope.NAMESANDSTATE, null);
				
				if (!(itype instanceof TCBooleanType))
				{
					exp.report(6007, "@LoopInvariant must be a boolean expression");
				}
			}
				
			if (!reasonsAbout.containsAll(updates))
			{
				// Invariant doesn't reason about some variable updated in the loop
				TCNameList missing = new TCNameList();
				missing.addAll(updates);
				missing.removeAll(reasonsAbout);
				stmt.warning(6007, "@LoopInvariants do not reason about " + missing);
			}

			if (!loopVars.isEmpty())	// So we must have at least one inv without these
			{
				boolean oneOkay = false;

				for (TCLoopInvariantAnnotation loopInv: invariants)
				{
					boolean hasLoopVars = false;
					TCNameSet invVars = loopInv.args.get(0).getVariableNames();

					for (TCNameToken loopVar: loopVars)
					{
						if (invVars.contains(loopVar))
						{
							hasLoopVars = true;
							break;
						}
					}
					
					if (!hasLoopVars)
					{
						oneOkay = true;
					}
					else
					{
						loopInv.setHasLoopVars();
					}
				}

				if (!oneOkay)
				{
					stmt.report(6007, "At least one @LoopInvariant must be independent of " + loopVars);
				}
			}
		}
	}
}