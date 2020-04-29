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
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
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
import rpc.RPCMessageList;
import rpc.RPCRequest;
import rpc.RPCResponse;
import vdmj.LSPDefinitionFinder;

public class WorkspaceManagerSL extends WorkspaceManager
{
	private ASTModuleList astModuleList = null;
	private TCModuleList tcModuleList = null;
	private INModuleList inModuleList = null;

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
			inModuleList = null;
		}
		
		errs.addAll(warns);
		return diagnosticResponses(errs, null);
	}

	@Override
	public RPCMessageList findDefinition(RPCRequest request, File file, int line, int col)
	{
		if (tcModuleList != null && !tcModuleList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			TCDefinition def = finder.find(tcModuleList, file, line + 1, col + 1);
			
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
		else
		{
			return new RPCMessageList(new RPCResponse(request, null));
		}
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
	public ModuleInterpreter getInterpreter() throws Exception
	{
		if (interpreter == null)
		{
			interpreter = new ModuleInterpreter(inModuleList, tcModuleList);
		}
		
		return (ModuleInterpreter) interpreter;
	}

	@Override
	protected boolean canExecute()
	{
		return inModuleList != null;
	}

	@Override
	public DAPMessageList terminate(DAPRequest request, Boolean restart)
	{
		interpreter = null;
		return super.terminate(request, restart);
	}
	
	@Override
	public DAPMessageList threads(DAPRequest request)
	{
		return new DAPMessageList(request, new JSONObject("threads", new JSONArray()));	// empty?
	}

	@Override
	public DAPMessageList evaluate(DAPRequest request, String expression, String context)
	{
		try
		{
			for (ASTModule m: astModuleList)
			{
				if (m.name.name.equals(expression))
				{
					interpreter.setDefaultName(expression);
					DAPMessageList responses = new DAPMessageList(request,
						new JSONObject("result", "Default module set to " + expression, "variablesReference", 0));
					prompt(responses);
					return responses;
				}
			}
			
			if (!canExecute())
			{
				DAPMessageList responses = new DAPMessageList(request,
						new JSONObject("result", "Cannot start interpreter: errors exist?", "variablesReference", 0));
				prompt(responses);
				return responses;
			}
			else if (getInterpreter().getIN() != inModuleList)
			{
				DAPMessageList responses = new DAPMessageList(request,
						new JSONObject("result", "Specification has changed: try restart", "variablesReference", 0));
				prompt(responses);
				return responses;
			}
			
			return super.evaluate(request, expression, context);
		}
		catch (Exception e)
		{
			DAPMessageList responses = new DAPMessageList(request, e);
			prompt(responses);
			return responses;
		}
	}
}
