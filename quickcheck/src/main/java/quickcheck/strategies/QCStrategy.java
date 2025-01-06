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

import java.util.List;

import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;

import quickcheck.QuickCheck;

abstract public class QCStrategy
{
	protected int errorCount = 0;

	public boolean hasErrors()
	{
		return errorCount > 0;
	}

	public String help()
	{
		return getName() + " (no options)";
	}

	public boolean useByDefault()
	{
		return true;
	}

	public boolean init(QuickCheck qc)
	{
		return true;
	}

	abstract public String getName();
	abstract public StrategyResults getValues(ProofObligation po, List<INBindingOverride> binds, Context ctxt);
	
	public void maybeHeuristic(ProofObligation po)
	{
		return;		// Should update po.message
	}
}
