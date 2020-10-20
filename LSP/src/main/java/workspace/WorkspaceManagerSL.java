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

package workspace;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.modules.POModuleList;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import lsp.textdocument.SymbolKind;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import vdmj.LSPDefinitionFinder;
import vdmj.LSPDefinitionFinder.Found;

public class WorkspaceManagerSL extends WorkspaceManager
{
	private ASTModuleList astModuleList = null;
	private TCModuleList tcModuleList = null;
	private INModuleList inModuleList = null;
	private POModuleList poModuleList = null;

	public WorkspaceManagerSL()
	{
		Settings.dialect = Dialect.VDM_SL;
	}
	
	@Override
	protected List<VDMMessage> parseFile(File file)
	{
		List<VDMMessage> errs = new Vector<VDMMessage>();
		StringBuilder buffer = projectFiles.get(file);
		
		LexTokenReader ltr = new LexTokenReader(buffer.toString(),
				Dialect.VDM_SL, file, Charset.defaultCharset().displayName());
		ModuleReader mr = new ModuleReader(ltr);
		mr.readModules();
		
		if (mr.getErrorCount() > 0)
		{
			errs.addAll(mr.getErrors());
		}
		
		if (mr.getWarningCount() > 0)
		{
			errs.addAll(mr.getWarnings());
		}

		Log.dump(errs);
		return errs;
	}

	@Override
	protected RPCMessageList checkLoadedFiles() throws Exception
	{
		astModuleList = new ASTModuleList();
		List<VDMMessage> errs = new Vector<VDMMessage>();
		List<VDMMessage> warns = new Vector<VDMMessage>();
		
		for (Entry<File, StringBuilder> entry: projectFiles.entrySet())
		{
			LexTokenReader ltr = new LexTokenReader(entry.getValue().toString(),
					Dialect.VDM_SL, entry.getKey(), Charset.defaultCharset().displayName());
			ModuleReader mr = new ModuleReader(ltr);
			astModuleList.addAll(mr.readModules());
			
			if (mr.getErrorCount() > 0)
			{
				errs.addAll(mr.getErrors());
			}
			
			if (mr.getWarningCount() > 0)
			{
				warns.addAll(mr.getWarnings());
			}
		}
		
		if (errs.isEmpty())
		{
			tcModuleList = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(astModuleList);
			tcModuleList.combineDefaults();
			TypeChecker tc = new ModuleTypeChecker(tcModuleList);
			tc.typeCheck();
			
			if (TypeChecker.getErrorCount() > 0)
			{
				errs.addAll(TypeChecker.getErrors());
			}
			
			if (TypeChecker.getWarningCount() > 0)
			{
				warns.addAll(TypeChecker.getWarnings());
			}
		}
		else
		{
			Log.error("Syntax errors found");
			Log.dump(errs);
			Log.dump(warns);
			tcModuleList = null;
		}
		
		if (errs.isEmpty())
		{
			inModuleList = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(tcModuleList);
		}
		else
		{
			Log.error("Type checking errors found");
			Log.dump(errs);
			Log.dump(warns);
			tcModuleList = null;
			inModuleList = null;
		}
		
		errs.addAll(warns);
		RPCMessageList result = diagnosticResponses(errs, null);
		
		if (hasClientCapability("experimental.proofObligationGeneration"))
		{
			poModuleList = null;
			result.add(new RPCRequest("lspx/POG/updated",
					new JSONObject("successful", tcModuleList != null)));
		}
		
		return result;
	}

	@Override
	protected TCNode findLocation(File file, int zline, int zcol)
	{
		if (tcModuleList != null && !tcModuleList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			Found found = finder.findLocation(tcModuleList, file, zline + 1, zcol + 1);
			
			if (found != null)
			{
				return found.node;
			}
		}

		return null;
	}

	@Override
	protected TCDefinition findDefinition(File file, int zline, int zcol)
	{
		if (tcModuleList != null && !tcModuleList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			return finder.findDefinition(tcModuleList, file, zline + 1, zcol + 1);
		}
		else
		{
			return null;
		}
	}

	@Override
	public RPCMessageList findDefinition(RPCRequest request, File file, int zline, int zcol)
	{
		TCDefinition def = findDefinition(file, zline, zcol);
		
		if (def == null)
		{
			return new RPCMessageList(request, null);
		}
		else
		{
			URI defuri = def.location.file.toURI();
			
			return new RPCMessageList(request,
				System.getProperty("lsp.lsp4e") != null ?
					new JSONArray(
						new JSONObject(
							"targetUri", defuri.toString(),
							"targetRange", Utils.lexLocationToRange(def.location),
							"targetSelectionRange", Utils.lexLocationToPoint(def.location)))
					:
					new JSONObject(
						"uri", defuri.toString(),
						"range", Utils.lexLocationToRange(def.location)));
		}
	}

	@Override
	protected TCDefinitionList lookupDefinition(String startsWith)
	{
		TCDefinitionList results = new TCDefinitionList();
		
		for (TCModule module: tcModuleList)
		{
			for (TCDefinition def: module.defs)
			{
				if (def.name != null && def.name.getName().startsWith(startsWith))
				{
					results.add(def);
				}
			}
		}
		
		return results;
	}

	@Override
	protected FilenameFilter getFilenameFilter()
	{
		return Dialect.VDM_SL.getFilter();
	}
	
	@Override
	protected String[] getFilenameFilters()
	{
		return new String[] { "**/*.vdm", "**/*.vdmsl" }; 
	}

	@Override
	public RPCMessageList documentSymbols(RPCRequest request, File file)
	{
		JSONArray results = new JSONArray();
		
		if (tcModuleList != null)	// May be syntax errors
		{
			for (TCModule module: tcModuleList)
			{
				if (module.files.contains(file))
				{
					results.add(symbolInformation(module.name.toString(), module.name.getLocation(), SymbolKind.Module, null));

					for (TCDefinition def: module.defs)
					{
						for (TCDefinition indef: def.getDefinitions())
						{
							if (indef.name != null && indef.location.file.equals(file) && !indef.name.isOld())
							{
								results.add(symbolInformation(indef.name + ":" + indef.getType(),
										indef.location, SymbolKind.kindOf(indef), indef.location.module));
							}
						}
					}
				}
			}
		}
		else if (astModuleList != null)		// Try AST instead
		{
			for (ASTModule module: astModuleList)
			{
				if (module.files.contains(file))
				{
					results.add(symbolInformation(module.name, SymbolKind.Module, null));

					for (ASTDefinition def: module.defs)
					{
						if (def.name != null && def.location.file.equals(file) && !def.name.old)
						{
							results.add(symbolInformation(def.name.toString(),
									def.name.location, SymbolKind.kindOf(def), def.location.module));
						}
					}
				}
			}
		}
		
		return new RPCMessageList(request, results);
	}

	@Override
	public ModuleInterpreter getInterpreter()
	{
		if (interpreter == null)
		{
			try
			{
				interpreter = new ModuleInterpreter(inModuleList, tcModuleList);
			}
			catch (Exception e)
			{
				Log.error(e);
				interpreter = null;
			}
		}
		
		return (ModuleInterpreter) interpreter;
	}

	@Override
	protected boolean canExecute()
	{
		return getInterpreter() != null;	// inModuleList != null;
	}
	
	@Override
	protected boolean hasChanged()
	{
		return getInterpreter() != null && getInterpreter().getIN() != inModuleList;
	}
	
	@Override
	public DAPMessageList threads(DAPRequest request)
	{
		return new DAPMessageList(request, new JSONObject("threads", new JSONArray()));	// empty?
	}

	@Override
	public RPCMessageList pogGenerate(RPCRequest request, File file, JSONObject range)
	{
		if (tcModuleList == null)	// No type clean tree
		{
			return new RPCMessageList(request, RPCErrors.InternalError, "Type checking errors found");
		}
		
		try
		{
			if (poModuleList == null)
			{
				poModuleList = ClassMapper.getInstance(PONode.MAPPINGS).init().convert(tcModuleList);
			}
			
			ProofObligationList poGeneratedList = poModuleList.getProofObligations();
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
						"source",	splitPO(po.value),
						"proved",	po.status != POStatus.UNPROVED));
			}
			
			return new RPCMessageList(request, results);
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
}
