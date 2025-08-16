/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package quickcheck.strategies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.values.ValueList;

/**
 * A class to hold the return values of a getValues() call on a QC plugin.
 * The (dis)provedBy fields indicate that the PO probably has no counterexamples.
 * Otherwise, counterexamples contains known or possible values to check. The
 * hasAllValues field indicates that all possible values of all bindings are included
 * (probably from the "finite" strategy) and hence if no counterexamples are found,
 * the PO is provable.
 */
public class StrategyResults
{
	public final Map<String, ValueList> possibleValues;
	public final StrategyUpdater updater;	// Explicitly set PO fields
	public final boolean hasAllValues;		// Contains all possible values from all binds
	
	public INExpression inExpression;		// The INExpression of the PO
	public List<INBindingOverride> binds;	// The binds used from the PO

	public StrategyResults()
	{
		this.possibleValues = new HashMap<String, ValueList>();
		this.updater = null;
		this.hasAllValues = false;
	}

	public StrategyResults(StrategyUpdater updater)
	{
		this.possibleValues = new HashMap<String, ValueList>();
		this.updater = updater;
		this.hasAllValues = false;
	}

	public StrategyResults(Map<String, ValueList> possibleValues, boolean hasAllValues)
	{
		this.possibleValues = possibleValues;
		this.updater = null;
		this.hasAllValues = hasAllValues;
	}

	public void setDetails(INExpression inExpression, List<INBindingOverride> binds)
	{
		this.inExpression = inExpression;
		this.binds = binds;
	}
}
