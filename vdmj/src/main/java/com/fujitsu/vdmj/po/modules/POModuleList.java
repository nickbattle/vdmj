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

package com.fujitsu.vdmj.po.modules;

import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("serial")
public class POModuleList extends POMappedList<TCModule, POModule>
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
		
		for (POModule m: this)
		{
			obligations.addAll(m.getProofObligations(menv));
		}

		return obligations;
	}
}
