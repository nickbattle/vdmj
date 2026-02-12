/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package workspace.plugins;

import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.annotations.POAnnotation;
import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.POClassList;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.util.NullProgress;
import com.fujitsu.vdmj.util.Progress;

import json.JSONObject;
import workspace.events.CheckPrepareEvent;

public class POPluginPR extends POPlugin
{
	private POClassList poClassList;

	public POPluginPR()
	{
		super();
	}

	@Override
	protected void preCheck(CheckPrepareEvent ev)
	{
		super.preCheck(ev);
		poClassList = null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Mappable> T getPO()
	{
		return (T) poClassList;
	}

	@Override
	public <T extends Mappable> boolean checkLoadedFiles(T tcList) throws Exception
	{
		poClassList = ClassMapper.getInstance(PONode.MAPPINGS).init().convert(tcList);
		return true;
	}

	@Override
	protected void addDependencyCodeLenses()
	{
		for (POClassDefinition clazz: poClassList)
		{
			createPOGDependencyLenses(clazz.definitions);
		}
	}

	@Override
	public ProofObligationList getProofObligations()
	{
		return getProofObligations(new NullProgress());
	}

	@Override
	public ProofObligationList getProofObligations(Progress progress)
	{
		if (obligationList == null)
		{
			POAnnotation.init();
			obligationList = poClassList.getProofObligations(progress);
			POAnnotation.close();

			if (!progress.cancelRequested())
			{
				obligationList.renumber();
				addDependencyCodeLenses();
			}
		}

		return obligationList;
	}

	@Override
	public JSONObject getCexLaunch(ProofObligation po)
	{
		return null;	// Needs to create new object?
	}

	@Override
	public JSONObject getWitnessLaunch(ProofObligation po)
	{
		return null;	// Needs to create new object?
	}

	@Override
	protected int getTotal()
	{
		return poClassList.getTotal();
	}
}
