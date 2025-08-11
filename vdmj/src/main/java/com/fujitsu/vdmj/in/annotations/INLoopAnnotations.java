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

package com.fujitsu.vdmj.in.annotations;

import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.Value;

public class INLoopAnnotations implements Mappable
{
	private final INLoopInvariantList invariants;
	private final INLoopMeasureAnnotation measure;

	public INLoopAnnotations(INLoopInvariantList invariants, INLoopMeasureAnnotation measure)
	{
		this.invariants = invariants;
		this.measure = measure;
	}

	// These methods are called by the various loop INStatements. Note that
	// the inBefore/inAfter methods in INLoopInvariantAnnotation are not
	// used, because all the invariants need to be treated as a whole.

	public void before(Context ctxt) throws ValueException
	{
		invariants.setGhostValue(ctxt);		// Add any ghost to ctxt

		if (measure != null)
		{
			measure.before(ctxt);
		}
	}

	public void check(Context ctxt, boolean inside) throws ValueException
	{
		for (INLoopInvariantAnnotation invariant: invariants)
		{
			invariant.check(ctxt, inside);
		}
	}

	public void checkWithMeasure(Context ctxt) throws ValueException
	{
		check(ctxt, true);	// Measures always inside

		if (measure != null)
		{
			measure.check(ctxt);
		}
	}

	public void checkUpdateGhost(Context ctxt, Value val) throws ValueException
	{
		invariants.updateGhostValue(ctxt, val);

		for (INLoopInvariantAnnotation invariant: invariants)
		{
			invariant.check(ctxt, true);		// Always inside
		}
	}

	public void after(Context ctxt)
	{
		invariants.removeGhostVariable(ctxt);

		if (measure != null)
		{
			measure.removeMeasure(ctxt);
		}
	}
}