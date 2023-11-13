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
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.modules.POModuleList;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingConstantVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.PORemoveIgnoresVisitor;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Context;

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
	protected JSONObject getLaunch(ProofObligation po, Context ctxt)
	{
		PODefinition def = po.definition;
		JSONObject result = null;
		
		if (def instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition efd = (POExplicitFunctionDefinition)def;
			StringBuilder callString = new StringBuilder();
			PORemoveIgnoresVisitor.init();
			
			for (POPatternList pl: efd.paramPatternList)
			{
				callString.append("(");
				String sep = "";
				
				for (POPattern p: pl)
				{
					String match = paramMatch(p.removeIgnorePatterns(), ctxt);
					
					if (match == null)
					{
						return null;	// Can't match some params
					}

					callString.append(sep);
					callString.append(match);
					sep = ", ";
				}
					
				callString.append(")");
			}
			
			result = new JSONObject(
				"name",		"PO #" + po.number + " counterexample",
				"type",		"vdm",
				"request",	"launch",
				"noDebug",	false,
				"defaultName",	def.name.getModule(),
				"command",	"print " + def.name.getName() + callString
			);
		}
		else if (def instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition efd = (POImplicitFunctionDefinition)def;
			StringBuilder callString = new StringBuilder();
			callString.append("(");
			String sep = "";
			PORemoveIgnoresVisitor.init();
			
			for (POPatternListTypePair pl: efd.parameterPatterns)
			{
				for (POPattern p: pl.patterns)
				{
					String match = paramMatch(p.removeIgnorePatterns(), ctxt);
					
					if (match == null)
					{
						return null;	// Can't match some params
					}

					callString.append(sep);
					callString.append(match);
					sep = ", ";
				}
			}
			
			callString.append(")");
			
			result = new JSONObject(
				"name",		"PO #" + po.number + " counterexample",
				"type",		"vdm",
				"request",	"launch",
				"noDebug",	false,
				"defaultName",	def.name.getModule(),
				"command",	"print " + def.name.getName() + callString
			);
		}

		return result;
	}

	private String paramMatch(POPattern p, Context ctxt)
	{
		POGetMatchingConstantVisitor visitor = new POGetMatchingConstantVisitor();
		String result = p.apply(visitor, ctxt);
		return visitor.hasFailed() ? null : result;
	}
}
