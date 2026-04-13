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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.plugins.HelpList;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.definitions.POTypeDefinition;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POType;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.visitors.TCApplyFinder;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.Progress;

import json.JSONArray;
import json.JSONObject;
import lsp.CancellableThread;
import lsp.Utils;
import lsp.lspx.POGHandler;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import vdmj.commands.AnalysisCommand;
import vdmj.commands.PogCommand;
import vdmj.commands.PogDepCommand;
import workspace.Diag;
import workspace.EventListener;
import workspace.MessageHub;
import workspace.events.CheckCompleteEvent;
import workspace.events.CheckPrepareEvent;
import workspace.events.CodeLensEvent;
import workspace.events.InlayHintEvent;
import workspace.events.LSPEvent;
import workspace.inlays.POInlayHint;
import workspace.inlays.POMissingPOInlayHint;
import workspace.lenses.POCodeLens;
import workspace.lenses.POPostDependencyLens;

abstract public class POPlugin extends AnalysisPlugin implements EventListener
{
	private static final int MIN_PROGRESSABLE = 20;		// Min defs before POG shows progress

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

	private final Map<File, List<POCodeLens>> codeLenses;
	private final Map<File, List<POInlayHint>> inlayHints;
	protected ProofObligationList obligationList;

	protected POPlugin()
	{
		super();
		
		codeLenses = new HashMap<File, List<POCodeLens>>();
		inlayHints = new HashMap<File, List<POInlayHint>>();
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
		eventhub.register(InlayHintEvent.class, this);
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
		else if (event instanceof InlayHintEvent)
		{
			InlayHintEvent ihe = (InlayHintEvent)event;
			return new RPCMessageList(ihe.request, getInlayHints(ihe.file, ihe.range));
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
		codeLenses.clear();
		obligationList = null;
	}

	/**
	 * Event handling above. Supporting methods below. 
	 */
	
	abstract public <T extends Mappable> T getPO();
	
	abstract public <T extends Mappable> boolean checkLoadedFiles(T poList) throws Exception;
	
	abstract public ProofObligationList getProofObligations();
	
	abstract public ProofObligationList getProofObligations(Progress progress);

	abstract protected int getTotal();
	
	abstract public JSONObject getCexLaunch(ProofObligation po);

	abstract public JSONObject getWitnessLaunch(ProofObligation po);
	
	private JSONArray splitPO(String value)
	{
		String[] parts = value.trim().split("\\n\\s+");
		JSONArray array = new JSONArray();
		
		for (String part: parts)
		{
			array.add(part);
		}
		
		return array;
	}

	public RPCMessageList pogGenerate(RPCRequest request, File file, JSONArray obligations)
	{
		try
		{
			if (messagehub.hasErrors())	// No clean tree
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Specification errors found");
			}

			if (CancellableThread.currentlyRunning() != null)
			{
				Diag.error("Running " + CancellableThread.currentlyRunning());
				return new RPCMessageList(request, RPCErrors.InternalError, "Still running " + CancellableThread.currentlyRunning());
			}

			if (obligationList == null)	// New POs will be generated
			{
				int total = getTotal();
				JSONObject params = request.get("params");
				String workDoneToken = params.get("workDoneToken");

				if (total > MIN_PROGRESSABLE && workDoneToken != null)
				{
					POGThread progressThread = new POGThread(request, file, obligations);
					progressThread.start();
					return null;
				}
			}

			RPCMessageList response = getPOGResponse(request, file, obligations);
			response.addAll(MessageHub.getInstance().getDiagnosticResponses());
			return response;
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public RPCMessageList getPOGResponse(RPCRequest request, File file, JSONArray obligations)
	{
		ProofObligationList full = getProofObligations();
		ProofObligationList chosen = new ProofObligationList();

		if (obligations != null && !obligations.isEmpty())
		{
			// Ignore file limitation

			for (int i=0; i < obligations.size(); i++)
			{
				long po = obligations.index(i);		// 1 to n

				if (po <= full.size())
				{
					chosen.add(full.get((int) po - 1));
				}
			}
		}
		else
		{
			for (ProofObligation po: full)
			{
				if (locationInScope(po.location, file))		// Null file matches everything
				{
					chosen.add(po);
				}
			}
		}

		return getJSONObligations(request, chosen);
	}

	abstract protected void addDependencyCodeLenses();

	public ProofObligationList getDependentPOs(TCNameToken applyname)
	{
		ProofObligationList result = new ProofObligationList();

		for (ProofObligation po: getProofObligations())
		{
			if (po.getCheckedExpression() != null)
			{
				TCExpressionList applies = po.getCheckedExpression().apply(new TCApplyFinder(), applyname);

				if (!applies.isEmpty())
				{
					result.add(po);
				}
			}
			else if (po.source.contains(applyname.getName() + "("))	// Unchecked POs?
			{
				result.add(po);
			}
		}

		return result;
	}

	private ProofObligationList getDependentPOs(PODefinition def, POType type)
	{
		ProofObligationList result = new ProofObligationList();

		for (ProofObligation po: getProofObligations())
		{
			if (po.definition == def && po.kind == type)
			{
				result.add(po);
			}
		}

		return result;
	}

	private void createOneLens(PODefinition def)
	{
		ProofObligationList dependencies = null;
		LexLocation loc = null;

		if (def != null)
		{
			dependencies = getDependentPOs(def.name);
			loc = def.location;
		}

		if (dependencies != null && !dependencies.isEmpty())
		{
			addCodeLens(loc.file, new POPostDependencyLens(def, dependencies));
		}
	}

	protected void createPOGDependencyLenses(PODefinitionList definitions)
	{
		for (PODefinition def: definitions)
		{
			if (def instanceof POExplicitOperationDefinition)
			{
				POExplicitOperationDefinition exop = (POExplicitOperationDefinition)def;
				createOneLens(exop.predef);
				createOneLens(exop.postdef);
				createOneLens(exop.measureDef);
			}
			else if (def instanceof POImplicitOperationDefinition)
			{
				POImplicitOperationDefinition imop = (POImplicitOperationDefinition)def;
				createOneLens(imop.predef);
				createOneLens(imop.postdef);
				createOneLens(imop.measureDef);
			}
			else if (def instanceof POTypeDefinition)
			{
				POTypeDefinition tdef = (POTypeDefinition)def;

				if (tdef.invdef != null)
				{
					ProofObligationList dependencies = getDependentPOs(tdef, POType.INV_SATISFIABILITY);
					dependencies.addAll(getDependentPOs(tdef.invdef.name));
					addCodeLens(tdef.invdef.location.file, new POPostDependencyLens(tdef.invdef, dependencies));
				}

				createOneLens(tdef.eqdef);
				createOneLens(tdef.orddef);
			}
			else if (def instanceof POExplicitFunctionDefinition)
			{
				POExplicitFunctionDefinition exfn = (POExplicitFunctionDefinition)def;
				createOneLens(exfn.predef);
				createOneLens(exfn.postdef);
				createOneLens(exfn.measureDef);
			}
			else if (def instanceof POImplicitFunctionDefinition)
			{
				POImplicitFunctionDefinition imfn = (POImplicitFunctionDefinition)def;
				createOneLens(imfn.predef);
				createOneLens(imfn.postdef);
				createOneLens(imfn.measureDef);
			}
			else if (def instanceof POStateDefinition)
			{
				POStateDefinition sdef = (POStateDefinition)def;

				if (sdef.invdef != null)
				{
					ProofObligationList dependencies = getDependentPOs(sdef.invdef, POType.STATE_INVARIANT);
					dependencies.addAll(getDependentPOs(sdef.invdef.name));
					addCodeLens(sdef.invdef.location.file, new POPostDependencyLens(sdef.invdef, dependencies));
				}

				if (sdef.initdef != null)
				{
					ProofObligationList dependencies = getDependentPOs(sdef.initdef, POType.STATE_INIT);
					dependencies.addAll(getDependentPOs(sdef.initdef.name));
					addCodeLens(sdef.initdef.location.file, new POPostDependencyLens(sdef.initdef, dependencies));
				}
			}
		}
	}

	private boolean locationInScope(LexLocation location, File file)
	{
		if (file != null)
		{
			if (location.file.getName().equals("console"))	// Some eq/ord POs
			{
				return true;
			}
			else if (file.isFile())
			{
				if (!location.file.equals(file))
				{
					return false;
				}
			}
			else if (file.isDirectory())
			{
				String path = file.getPath();
				
				if (!location.file.getPath().startsWith(path))
				{
					return false;
				}
			}
		}

		return true;
	}

	private RPCMessageList getJSONObligations(RPCRequest request, ProofObligationList chosen)
	{
		JSONArray poList = new JSONArray();
		
		for (ProofObligation po: chosen)
		{
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

			JSONArray hovers = new JSONArray("name", "status");

			// Add the message, if we have one
			if (po.message != null)
			{
				source = po.message + "\n----\n" + source;
				hovers.add("message");
			}

			JSONObject json = new JSONObject(
				"type",		"PO",
					"id",		Long.valueOf(po.number),
					"kind", 	po.kind.toString(),
					"name",		name,
					"location",	Utils.lexLocationToLocation(po.location),
					"source",	splitPO(source),
					"status",	po.status.toString(),
					"message", 	po.message,
					"hovers",	hovers);
			
			poList.add(json);
		}

		poList.addAll(getJSONReductions());

		return new RPCMessageList(request, poList);
	}

	private JSONArray getJSONReductions()
	{
		JSONArray rlist = new JSONArray();

		// Add dummy POs/InlayHints for any operations with missing POs.
		Map<PODefinition,Long> reduced = POContextStack.getReducedDefinitions();
		inlayHints.clear();
		
		for (PODefinition def: reduced.keySet())
		{
			long paths = reduced.get(def);
			long missing = paths - Properties.pog_max_alt_paths;

			JSONObject reduction = new JSONObject(
					"type",		"missing",
					"name",		new JSONArray(def.name.getModule(), def.name.getName()),
					"location",	Utils.lexLocationToLocation(def.location),
					"message",	"Definition '" + def.name.getName() + "' too complex: " +
								missing + " of " + paths + " paths missing",
					"paths",	paths);

			rlist.add(reduction);
			addInlayHint(def.location.file,
				new POMissingPOInlayHint(def.location, "\u26A0\uFE0F", getMissingPOMarkup(paths, missing)));
		}

		return rlist;
	}

	private String getMissingPOMarkup(long paths, long missing)
	{
		return
			"### This definition is too complex for POG\n" +
			"There are " +
			paths +
			" possible execution paths through this definition, " +
			"which exceeds the configured limit in the Java property vdmj.pog.max_alt_paths (" +
			Properties.pog_max_alt_paths +
			")\n\n" +
			"The proof obligation generation (POG) has therefore omitted " + missing +
			" POs. Try to simplify the definition, or increase the property value.";
	}

	public void clearLenses(Class<?> type)
	{
		for (File file: codeLenses.keySet())
		{
			List<POCodeLens> lenses = codeLenses.get(file);
			Iterator<POCodeLens> iter = lenses.iterator();

			while (iter.hasNext())
			{
				POCodeLens lens = iter.next();

				if (type.isAssignableFrom(lens.getClass()))
				{
					iter.remove();
				}
			}
		}
	}
	
	public void addCodeLens(File file, POCodeLens lens)
	{
		List<POCodeLens> array = codeLenses.get(file);
		
		if (array == null)
		{
			array = new Vector<POCodeLens>();
			codeLenses.put(file, array);
		}
		
		array.add(lens);
	}

	private JSONArray getCodeLenses(File file)
	{
		JSONArray results = new JSONArray();
		
		if (codeLenses.containsKey(file))
		{
			for (POCodeLens lens: codeLenses.get(file))
			{
				results.addAll(lens.getLaunchLens());
			}
		}
		
		return results;
	}
	
	public void addInlayHint(File file, POInlayHint lens)
	{
		List<POInlayHint> array = inlayHints.get(file);
		
		if (array == null)
		{
			array = new Vector<POInlayHint>();
			inlayHints.put(file, array);
		}
		
		array.add(lens);
	}

	private JSONArray getInlayHints(File file, JSONObject range)
	{
		JSONArray results = new JSONArray();
		
		if (inlayHints.containsKey(file))
		{
			for (POInlayHint hint: inlayHints.get(file))
			{
				results.add(hint.getInlayHint());
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

			case "pogdep":
				return new PogDepCommand(line);

			default:
				return null;
		}
	}

	@Override
	public HelpList getCommandHelp()
	{
		return new HelpList
		(
			PogCommand.HELP, PogDepCommand.HELP
		);	
	}
}
