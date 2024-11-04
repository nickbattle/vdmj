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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package workspace.plugins;

import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.annotations.POAnnotation;
import com.fujitsu.vdmj.po.modules.POModuleList;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import json.JSONObject;
import workspace.events.CheckPrepareEvent;

public class POPluginSL extends POPlugin
{
	private POModuleList poModuleList;
	private ProofObligationList obligationList;

	public POPluginSL()
	{
		super();
	}

	@Override
	public void preCheck(CheckPrepareEvent ev)
	{
		super.preCheck(ev);
		poModuleList = null;
		obligationList = null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Mappable> T getPO()
	{
		return (T) poModuleList;
	}

	@Override
	public <T extends Mappable> boolean checkLoadedFiles(T tcList) throws Exception
	{
		poModuleList = ClassMapper.getInstance(PONode.MAPPINGS).init().convert(tcList);
		return true;
	}

	@Override
	public ProofObligationList getProofObligations()
	{
		if (obligationList == null)
		{
			POAnnotation.init();
			obligationList = poModuleList.getProofObligations();
			POAnnotation.close();
			obligationList.renumber();
		}
		
		return obligationList;
	}

	@Override
	public JSONObject getCexLaunch(ProofObligation po)
	{
		String launch = po.getCexLaunch();
		
		if (launch == null)
		{
			return null;	// No counterexample or definition or mismatched params
		}
		
		return new JSONObject(
				"name",			"PO #" + po.number + " counterexample",
				"type",			"vdm",
				"request",		"launch",
				"noDebug",		false,
				"defaultName",	po.location.module,
				"command",		"print " + launch
			);
	}

	@Override
	public JSONObject getWitnessLaunch(ProofObligation po)
	{
		String launch = po.getWitnessLaunch();
		
		if (launch == null)
		{
			return null;	// No witness or definition or mismatched params
		}
		
		return new JSONObject(
				"name",			"PO #" + po.number + " witness",
				"type",			"vdm",
				"request",		"launch",
				"noDebug",		false,
				"defaultName",	po.location.module,
				"command",		"print " + launch
			);
	}
}
