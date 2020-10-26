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
 *
 ******************************************************************************/

package workspace.plugins;

import java.io.File;

import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import json.JSONArray;
import json.JSONObject;
import lsp.Utils;

abstract public class POPlugin extends AnalysisPlugin
{
	public POPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "PO";
	}

	@Override
	public void init()
	{
	}

	abstract public void preCheck();

	abstract public <T> T getPO();
	
	abstract public <T> boolean checkLoadedFiles(T poList) throws Exception;
	
	abstract protected ProofObligationList getProofObligations();
	
	public JSONArray getObligations(File file)
	{
		ProofObligationList poGeneratedList = getProofObligations();
		poGeneratedList.renumber();
		JSONArray results = new JSONArray();
		
		for (ProofObligation po: poGeneratedList)
		{
			if (file != null &&
				!po.location.file.equals(file) &&
				!po.location.file.getParentFile().equals(file))		// folder
			{
				continue;
			}
			
			JSONArray name = new JSONArray(po.location.module);
			
			for (String part: po.name.split(";\\s+"))
			{
				name.add(part);
			}

			results.add(
				new JSONObject(
					"id",		new Long(po.number),
					"kind", 	po.kind.toString(),
					"name",		name,
					"location",	Utils.lexLocationToLocation(po.location),
					"source",	po.value,
					"proved",	po.status != POStatus.UNPROVED));
		}
		
		return results;
	}
}
