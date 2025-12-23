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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.plugins.HelpList;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import lsp.lspx.POGHandler;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import vdmj.commands.AnalysisCommand;
import vdmj.commands.PogCommand;
import workspace.Diag;
import workspace.EventListener;
import workspace.MessageHub;
import workspace.events.CheckCompleteEvent;
import workspace.events.CheckPrepareEvent;
import workspace.events.CodeLensEvent;
import workspace.events.LSPEvent;
import workspace.lenses.POLaunchDebugLens;

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

	private final Map<File, List<POLaunchDebugLens>> codeLenses;

	protected POPlugin()
	{
		super();
		
		codeLenses = new HashMap<File, List<POLaunchDebugLens>>();
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
		lspDispatcher.register(new POGHandler(), "slsp/POG/generate");

		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckCompleteEvent.class, this);
		eventhub.register(CodeLensEvent.class, this);
	}
	
	@Override
	public void setServerCapabilities(JSONObject capabilities)
	{
		JSONObject experimental = capabilities.get("experimental");
		
		if (experimental != null)
		{
			experimental.put("proofObligationProvider", new JSONObject());
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
		else if (event instanceof CheckCompleteEvent)
		{
			TCPlugin tc = registry.getPlugin("TC");
			checkLoadedFiles(tc.getTC());
			RPCMessageList results = new RPCMessageList();
			results.add(RPCRequest.notification("slsp/POG/updated",
					new JSONObject("successful", !messagehub.hasErrors())));
			return results;
		}
		else if (event instanceof CodeLensEvent)
		{
			CodeLensEvent le = (CodeLensEvent)event;
			return new RPCMessageList(le.request, getCodeLenses(le.file));
		}
		else
		{
			Diag.error("Unhandled %s event %s", getName(), event);
			return null;
		}
	}

	protected void preCheck(CheckPrepareEvent event)
	{
		messagehub.clearPluginMessages(this);
		clearLenses();
	}

	/**
	 * Event handling above. Supporting methods below. 
	 */
	
	abstract public <T extends Mappable> T getPO();
	
	abstract public <T extends Mappable> boolean checkLoadedFiles(T poList) throws Exception;
	
	abstract public ProofObligationList getProofObligations();
	
	abstract public JSONObject getCexLaunch(ProofObligation po);

	abstract public JSONObject getWitnessLaunch(ProofObligation po);
	
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

	public RPCMessageList pogGenerate(RPCRequest request, File file)
	{
		try
		{
			if (messagehub.hasErrors())	// No clean tree
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Specification errors found");
			}
			
			return getJSONObligations(request, file);
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public RPCMessageList getJSONObligations(RPCRequest request, File file)
	{
		JSONArray poList = new JSONArray();
		
		for (ProofObligation po: getProofObligations())
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

			// Use the annotated explanation, if we have one.
			String source = po.getExplanation();

			if (source == null)
			{
				source = po.getSource();
			}

			// Add the message, if we have one
			if (po.message != null)
			{
				source = po.message + "\n----\n" + source;
			}

			JSONObject json = new JSONObject(
					"id",		Long.valueOf(po.number),
					"kind", 	po.kind.toString(),
					"name",		name,
					"location",	Utils.lexLocationToLocation(po.location),
					"source",	splitPO(source),
					"status",	po.status.toString());
			
			poList.add(json);
		}

		// Add dummy POs for any operations with missing POs.
		Map<PODefinition,Long> reduced = POContextStack.getReducedDefinitions();

		for (PODefinition def: reduced.keySet())
		{
			long paths = reduced.get(def);

			poList.add(new JSONObject(
					"id",		0,		// Appears at the start of the list
					"kind", 	"Missing POs",
					"name",		new JSONArray(def.name.getModule(), def.name.getName()),
					"location",	Utils.lexLocationToLocation(def.location),
					"source",	new JSONArray(
						"Operation is too complex (" + paths + " paths). Some POs missing."),
					"status",	POStatus.FAILED.toString()));
		}

		RPCMessageList response = new RPCMessageList(request, poList);
		response.addAll(MessageHub.getInstance().getDiagnosticResponses());
		
		return response;
	}

	public void clearLenses()
	{
		codeLenses.clear();
	}
	
	public void addCodeLens(ProofObligation po)
	{
		List<POLaunchDebugLens> array = codeLenses.get(po.location.file);
		
		if (array == null)
		{
			array = new Vector<POLaunchDebugLens>();
			codeLenses.put(po.location.file, array);
		}
		
		array.add(new POLaunchDebugLens(po));
	}

	private JSONArray getCodeLenses(File file)
	{
		JSONArray results = new JSONArray();
		
		if (codeLenses.containsKey(file))
		{
			for (POLaunchDebugLens lens: codeLenses.get(file))
			{
				results.addAll(lens.getLaunchLens());
			}
		}
		
		return results;
	}

	@Override
	public AnalysisCommand getCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		switch (parts[0])
		{
			case "pog":
				return new PogCommand(line);

			default:
				return null;
		}
	}

	@Override
	public HelpList getCommandHelp()
	{
		return new HelpList
		(
			PogCommand.HELP
		);	
	}
}
