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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package quickcheck.strategies;

import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.values.ValueList;

/**
 * A class to hold the return values of a getValues() call on a QC plugin.
 * The provedBy field indicates that the PO has been proved to have no counterexamples.
 * Otherwise, counterexamples contains known or possible values to check. The
 * hasAllValues field indicates that all possible values of all bindings are included
 * (probably from the "finite" strategy) and hence if no counterexamples are found,
 * the PO is proved.
 */
public class StrategyResults
{
	public final String provedBy;			// If set, proved already
	public boolean hasAllValues;			// Contains all possible values from all binds
	public final Map<String, ValueList> counterexamples;
	public final long duration;				// time to generate counterexamples, in millisecs

	public StrategyResults()
	{
		this.provedBy = null;
		this.hasAllValues = false;
		this.counterexamples = new HashMap<String, ValueList>();
		this.duration = 0;
	}

	public StrategyResults(String proved, boolean hasAllValues, Map<String, ValueList> counterexamples, long duration)
	{
		this.provedBy = proved;
		this.hasAllValues = hasAllValues;
		this.counterexamples = counterexamples;
		this.duration = duration;
	}
}
