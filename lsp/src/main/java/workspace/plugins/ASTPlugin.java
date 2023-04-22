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
import java.io.FilenameFilter;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTPerSyncDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTStateDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTTypeDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTValueDefinition;
import com.fujitsu.vdmj.ast.patterns.ASTIdentifierPattern;
import com.fujitsu.vdmj.ast.types.ASTField;
import com.fujitsu.vdmj.ast.types.ASTNamedType;
import com.fujitsu.vdmj.ast.types.ASTRecordType;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.Mappable;

import json.JSONArray;
import json.JSONObject;
import lsp.textdocument.SymbolKind;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.EventListener;
import workspace.LSPWorkspaceManager;
import workspace.events.ChangeFileEvent;
import workspace.events.CheckPrepareEvent;
import workspace.events.CheckSyntaxEvent;
import workspace.events.CodeLensEvent;
import workspace.events.InitializedEvent;
import workspace.events.LSPEvent;
import workspace.lenses.ASTCodeLens;
import workspace.lenses.ASTLaunchDebugLens;

public abstract class ASTPlugin extends AnalysisPlugin implements EventListener
{
	protected static final boolean STRUCTURED_OUTLINE = true;
	protected boolean dirty;
	
	public static ASTPlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			case VDM_SL:
				return new ASTPluginSL();
				
			case VDM_PP:
			case VDM_RT:
				return new ASTPluginPR();
				
			default:
				Diag.error("Unsupported dialect " + dialect);
				throw new IllegalArgumentException("Unsupported dialect: " + dialect);
		}
	}

	protected ASTPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "AST";
	}
	
	@Override
	public int getPriority()
	{
		return AST_PRIORITY;
	}

	@Override
	public void init()
	{
		eventhub.register(InitializedEvent.class, this);
		eventhub.register(ChangeFileEvent.class, this);
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckSyntaxEvent.class, this);
		eventhub.register(CodeLensEvent.class, this);
		this.dirty = false;
	}
	
	@Override
	public RPCMessageList handleEvent(LSPEvent event) throws Exception
	{
		if (event instanceof InitializedEvent)
		{
			return lspDynamicRegistrations();
		}
		else if (event instanceof ChangeFileEvent)
		{
			return didChange((ChangeFileEvent) event);
		}
		else if (event instanceof CodeLensEvent)
		{
			CodeLensEvent le = (CodeLensEvent)event;
			return new RPCMessageList(le.request, getCodeLenses(le.file));
		}
		else if (event instanceof CheckPrepareEvent)
		{
			preCheck((CheckPrepareEvent)event);
			return null;
		}
		else if (event instanceof CheckSyntaxEvent)
		{
			checkLoadedFiles((CheckSyntaxEvent)event);
			return null;
		}
		else
		{
			Diag.error("Unhandled %s event %s", getName(), event);
			return null;
		}
	}
	
	private RPCMessageList lspDynamicRegistrations()
	{
		RPCMessageList registrations = new RPCMessageList();
		LSPWorkspaceManager manager = LSPWorkspaceManager.getInstance();
		
		if (manager.hasClientCapability("workspace.didChangeWatchedFiles.dynamicRegistration"))
		{
			JSONArray watchers = new JSONArray();
			
			// Add the rootUri so that we only notice changes in our own project.
			// We watch for all files/dirs and deal with filtering in changedWatchedFiles,
			// otherwise directory deletions are not notified.
			watchers.add(new JSONObject("globPattern", manager.getRoot().getAbsolutePath() + "/**"));
			
			registrations.add( RPCRequest.create("client/registerCapability",
				new JSONObject(
					"registrations",
						new JSONArray(
							new JSONObject(
								"id", "12345",
								"method", "workspace/didChangeWatchedFiles",
								"registerOptions",
									new JSONObject("watchers", watchers)
				)))));
			
			Diag.info("Added dynamic registration for workspace/didChangeWatchedFiles");
		}
		else
		{
			Diag.info("Client does not support dynamic registration for workspace/didChangeWatchedFiles");
		}
		
		return registrations;
	}
	
	abstract protected void parseFile(File file);

	private RPCMessageList didChange(ChangeFileEvent event) throws Exception
	{
		messagehub.clearPluginMessages(this);
		parseFile(event.file);
		return messagehub.getDiagnosticResponses(event.file);	// Includes TC errs etc
	}
	
	protected void preCheck(CheckPrepareEvent ev)
	{
		messagehub.clearPluginMessages(this);
	}
	
	/**
	 * Event handling above. Supporting methods below. 
	 */
	abstract public void checkLoadedFiles(CheckSyntaxEvent ev);
	
	/**
	 * We register the launch/debug code lens here, if the tree is dirty. Else it
	 * is registered by the TCPlugin.
	 */
	protected List<ASTCodeLens> getASTCodeLenses(boolean dirty)
	{
		List<ASTCodeLens> lenses = new Vector<ASTCodeLens>();
		
		if (dirty)
		{
			lenses.add(new ASTLaunchDebugLens());
		}
		
		return lenses;
	}
	
	abstract protected JSONArray getCodeLenses(File file);

	abstract public <T extends Mappable> T getAST();
	
	public boolean isDirty()
	{
		return dirty;
	}
	
	abstract public JSONArray documentSymbols(File file);

	abstract public FilenameFilter getFilenameFilter();

	abstract public String[] getFilenameFilters();
	
	/**
	 * Common methods for hierarchical outlines.
	 */
	protected JSONArray documentSymbols(ASTDefinitionList defs)
	{
		JSONArray symbols = new JSONArray();

		for (ASTDefinition def: defs)
		{
			symbols.add(documentSymbolsDef(def));
		}
		
		return symbols;
	}

	private JSONObject documentSymbolsDef(ASTDefinition def)
	{
		String name = def.name == null ? null : def.name.toString();
		SymbolKind kind = SymbolKind.kindOf(def);
		
		if (name == null)
		{
			if (def instanceof ASTValueDefinition)
			{
				ASTValueDefinition vdef = (ASTValueDefinition)def;
				name = vdef.pattern.toString();
				
				if (!(vdef.pattern instanceof ASTIdentifierPattern))
				{
					kind = SymbolKind.Struct;
				}
			}
			else
			{
				name = def.toString();
			}
		}
		
		String detail = "";
		JSONArray children = null;
		
		if (def instanceof ASTExplicitFunctionDefinition)
		{
			ASTExplicitFunctionDefinition fdef = (ASTExplicitFunctionDefinition)def;
			detail = fdef.type.toString();
		}
		else if (def instanceof ASTImplicitFunctionDefinition)
		{
			ASTImplicitFunctionDefinition fdef = (ASTImplicitFunctionDefinition)def;
			detail = fdef.parameterPatterns + " ==> " + fdef.result;
		}
		else if (def instanceof ASTExplicitOperationDefinition)
		{
			ASTExplicitOperationDefinition opdef = (ASTExplicitOperationDefinition)def;
			detail = opdef.type.toString();
		}
		else if (def instanceof ASTImplicitOperationDefinition)
		{
			ASTImplicitOperationDefinition opdef = (ASTImplicitOperationDefinition)def;
			detail = opdef.parameterPatterns + " ==> " + opdef.result;
		}
		else if (def instanceof ASTStateDefinition)
		{
			ASTStateDefinition sdef = (ASTStateDefinition)def;
			children = new JSONArray();
			
			for (ASTField field: sdef.fields)
			{
				children.add(messages.documentSymbol(
						field.tag,
						field.type.toString(),
						SymbolKind.Field,
						field.tagname.location,
						field.tagname.location));
			}
		}
		else if (def instanceof ASTTypeDefinition)
		{
			ASTTypeDefinition tdef = (ASTTypeDefinition)def;
			
			if (tdef.type instanceof ASTNamedType)
			{
				ASTNamedType ntype = (ASTNamedType)tdef.type;
				detail = ntype.type.toString();
			}
			else if (tdef.type instanceof ASTRecordType)
			{
				ASTRecordType rtype = (ASTRecordType)tdef.type;
				children = new JSONArray();
				
				for (ASTField field: rtype.fields)
				{
					children.add(messages.documentSymbol(
							field.tag,
							field.type.toString(),
							SymbolKind.Field,
							field.tagname.location,
							field.tagname.location));
				}
			}
		}
		else if (def instanceof ASTPerSyncDefinition)
		{
			name = def.toString();
		}

		// Replace "(unresolved TypeName)" with "TypeName"
		detail = detail.replaceAll("\\(unresolved ([^)]+)\\)", "$1");
		
		return messages.documentSymbol(
			name,
			detail,
			kind,
			def.location,
			def.location,
			children);
	}
}
