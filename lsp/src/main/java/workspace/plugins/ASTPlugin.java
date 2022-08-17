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
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
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
import com.fujitsu.vdmj.messages.VDMMessage;

import json.JSONArray;
import json.JSONObject;
import lsp.textdocument.SymbolKind;
import rpc.RPCMessageList;
import workspace.Diag;
import workspace.EventListener;
import workspace.events.ChangeFileEvent;
import workspace.events.Event;
import workspace.lenses.ASTLaunchDebugLens;
import workspace.lenses.CodeLens;

public abstract class ASTPlugin extends AnalysisPlugin implements EventListener
{
	protected static final boolean STRUCTURED_OUTLINE = true;

	protected final List<VDMMessage> errs = new Vector<VDMMessage>();
	protected final List<VDMMessage> warns = new Vector<VDMMessage>();
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
				Diag.error("Unknown dialect " + dialect);
				throw new RuntimeException("Unsupported dialect: " + Settings.dialect);
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
	public void init()
	{
		eventhub.register(this, "textDocument/didChange", this);
		this.dirty = false;
	}
	
	/**
	 * We register the launch/debug code lens here, if the tree is dirty. Else it
	 * is registered by the TCPlugin.
	 */
	@Override
	protected List<CodeLens> getCodeLenses(boolean dirty)
	{
		List<CodeLens> lenses = new Vector<CodeLens>();
		
		if (dirty)
		{
			lenses.add(new ASTLaunchDebugLens());
		}
		
		return lenses;
	}

	@Override
	public RPCMessageList handleEvent(Event event) throws Exception
	{
		if (event instanceof ChangeFileEvent)
		{
			return didChange((ChangeFileEvent) event);
		}
		else
		{
			Diag.error("Unhandled ASTPlugin event %s", event);
			return new RPCMessageList();
		}
	}
	
	private RPCMessageList didChange(ChangeFileEvent event) throws Exception
	{
		List<VDMMessage> errors = parseFile(event.file);
		
		// Add TC errors as these need to be seen until the next save
		TCPlugin tc = registry.getPlugin("TC");
		errors.addAll(tc.getErrs());
		errors.addAll(tc.getWarns());
		
		// We report on this file, plus the files with tc errors (if any).
		Set<File> files = messages.filesOfMessages(errors);
		files.add(event.file);
		return messages.diagnosticResponses(errors, files);
	}
	
	public void preCheck()
	{
		errs.clear();
		warns.clear();
	}
	
	abstract public boolean checkLoadedFiles();
	
	public List<VDMMessage> getErrs()
	{
		return errs;
	}
	
	public List<VDMMessage> getWarns()
	{
		return warns;
	}
	
	abstract public <T extends Mappable> T getAST();
	
	abstract protected List<VDMMessage> parseFile(File file);

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
