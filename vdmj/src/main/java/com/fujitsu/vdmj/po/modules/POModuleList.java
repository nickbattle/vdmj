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

package com.fujitsu.vdmj.po.modules;

import java.util.concurrent.atomic.AtomicInteger;

import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.po.POProgress;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.util.Utils;

public class POModuleList extends POMappedList<TCModule, POModule> implements POProgress
{
	public POModuleList(TCModuleList from) throws Exception
	{
		super(from);
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}

	public ProofObligationList getProofObligations()
	{
		ProofObligationList obligations = new ProofObligationList();
		MultiModuleEnvironment menv = new MultiModuleEnvironment(this);
		POContextStack.reset();
		resetProgress();
		
		for (POModule m: this)
		{
			obligations.addAll(m.getProofObligations(this, menv));

			if (cancelRequested())
			{
				return new ProofObligationList();	// empty
			}
		}

		return obligations;
	}

	/**
	 * Count the number of top level definitions across all modules. This is
	 * used to calculate the progress of the POG for large specifications.
	 */
	private final AtomicInteger progress = new AtomicInteger();
	private boolean cancelled = false;

	public int getTotal()
	{
		int total = 0;

		for (POModule m: this)
		{
			if (m.defs != null)
			{
				total = total + m.defs.size();
			}
		}

		return total;
	}

	public int getProgress()
	{
		return progress.get();
	}

	@Override
	public synchronized void cancelProgress()
	{
		cancelled = true;
	}

	@Override
	public synchronized boolean cancelRequested()
	{
		return cancelled;
	}

	@Override
	public void resetProgress()
	{
		progress.set(0);
		cancelled = false;
	}

	@Override
	public void makeProgress(int n)
	{
		progress.addAndGet(n);

		/**
		 * TODO Remove this when we're happy with POG progress
		 */
		int delay = Integer.getInteger("lsp.pog.delay", 0);

		if (delay > 0)
		{
			try
			{
				Thread.sleep(delay);
			}
			catch (InterruptedException e)
			{
				// ?
			}
		}
	}
}
