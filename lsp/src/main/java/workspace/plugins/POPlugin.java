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

import java.io.File;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.EventListener;
import workspace.events.CheckCompleteEvent;
import workspace.events.CheckPrepareEvent;
import workspace.events.LSPEvent;

abstract public class POPlugin extends AnalysisPlugin implements EventListener
{
	public static POPlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			case VDM_SL:
				return new POPluginSL();
				
			case VDM_PP:
			case VDM_RT:
				return new POPluginPR();
				
			default:
				Diag.error("Unsupported dialect " + dialect);
				throw new IllegalArgumentException("Unsupported dialect: " + dialect);
		}
	}

	protected POPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "PO";
	}
	
	@Override
	public int getPriority()
	{
		return PO_PRIORITY;
	}

	@Override
	public void init()
	{
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckCompleteEvent.class, this);
	}
	
	@Override
	public void setServerCapabilities(JSONObject capabilities)
	{
		JSONObject experimental = capabilities.get("experimental");
		
		if (experimental != null)
		{
			experimental.put("proofObligationProvider", true);
		}
	}

	@Override
	public RPCMessageList handleEvent(LSPEvent event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			preCheck((CheckPrepareEvent)event);
			return new RPCMessageList();
		}
		if (event instanceof CheckCompleteEvent)
		{
			TCPlugin tc = registry.getPlugin("TC");
			checkLoadedFiles(tc.getTC());
			RPCMessageList results = new RPCMessageList();
			results.add(RPCRequest.notification("slsp/POG/updated",
					new JSONObject("successful", !messagehub.hasErrors())));
			return results;
		}
		else
		{
			Diag.error("Unhandled %s event %s", getName(), event);
			return null;
		}
	}

	protected void preCheck(CheckPrepareEvent event)
	{
		// Nothing
	}

	/**
	 * Event handling above. Supporting methods below. 
	 */
	
	abstract public <T extends Mappable> T getPO();
	
	abstract public <T extends Mappable> boolean checkLoadedFiles(T poList) throws Exception;
	
	abstract public ProofObligationList getProofObligations();
	
	protected JSONArray splitPO(String value)
	{
		String[] parts = value.trim().split("\\n\\s+");
		JSONArray array = new JSONArray();
		
		for (String part: parts)
		{
			array.add(part);
		}
		
		return array;
	}

	public RPCMessageList getJSONObligations(RPCRequest request, File file)
	{
		ProofObligationList poGeneratedList = getProofObligations();
		JSONArray poList = new JSONArray();
		
		messagehub.clearPluginMessages(this);
		List<VDMMessage> messages = new Vector<VDMMessage>();
		
		for (ProofObligation po: poGeneratedList)
		{
			if (file != null)
			{
				if (file.isFile())
				{
					if (!po.location.file.equals(file))
					{
						continue;
					}
				}
				else if (file.isDirectory())
				{
					String path = file.getPath();
					
					if (!po.location.file.getPath().startsWith(path))
					{
						continue;
					}
				}
			}
			
			JSONArray name = new JSONArray(po.location.module);
			
			for (String part: po.name.split(";\\s+"))
			{
				name.add(part);
			}

			JSONObject json = new JSONObject(
					"id",		Long.valueOf(po.number),
					"kind", 	po.kind.toString(),
					"name",		name,
					"location",	Utils.lexLocationToLocation(po.location),
					"source",	splitPO(po.value),
					"status",	po.status.toString());
			
			if (!po.counterexample.isEmpty())
			{
				JSONObject values = new JSONObject();
				
				for (TCNameToken vname: po.counterexample.keySet())
				{
					values.put(vname.getName(), po.counterexample.get(vname).toString());
				}
				
				json.put("counterexample", values);
				
				StringBuilder sb = new StringBuilder();
				sb.append("PO #");
				sb.append(po.number);
				sb.append(" counterexample: ");
				String sep = "";
				
				for (TCNameToken vname: po.counterexample.keySet())
				{
					sb.append(sep);
					sb.append(vname.getName());
					sb.append(" = ");
					sb.append(po.counterexample.get(vname));
					sep = ", ";
				}
				
				messages.add(new VDMWarning(9000, sb.toString(), po.location));
			}
			
			if (po.message != null)
			{
				json.put("message", "PO #" + po.number + ": " + po.message);
				messages.add(new VDMWarning(9001, po.message, po.location));
			}
			
			if (po.witness != null)
			{
				json.put("witness", "PO #" + po.number + ": " + po.witness);
			}
			
			poList.add(json);
		}
		
		messagehub.addPluginMessages(this, messages);
		RPCMessageList result = new RPCMessageList(request, poList);
		result.addAll(messagehub.getDiagnosticResponses());
		
		return result;
	}
}
