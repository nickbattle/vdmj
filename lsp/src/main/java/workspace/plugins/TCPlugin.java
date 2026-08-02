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
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCMutexSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCPerSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCThreadDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import json.JSONArray;
import json.JSONObject;
import lsp.textdocument.SymbolKind;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.EventListener;
import workspace.events.CheckPrepareEvent;
import workspace.events.CheckTypeEvent;
import workspace.events.CodeLensEvent;
import workspace.events.InlayHintEvent;
import workspace.events.LSPEvent;
import workspace.inlays.InlayHint;
import workspace.inlays.TCInlayHint;
import workspace.lenses.TCCodeLens;
import workspace.lenses.TCLaunchDebugLens;

abstract public class TCPlugin extends AnalysisPlugin implements EventListener
{
	public static TCPlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			case VDM_SL:
				return new TCPluginSL();
				
			case VDM_PP:
			case VDM_RT:
				return new TCPluginPR();
				
			default:
				Diag.error("Unsupported dialect " + dialect);
				throw new IllegalArgumentException("Unsupported dialect: " + dialect);
		}
	}

	protected final Map<File, JSONArray> codeLenses;		// cache for efficiency
	protected final Map<File, List<TCInlayHint>> inlayHints;		// cache for efficiency

	protected TCPlugin()
	{
		super();

		codeLenses = new HashMap<File, JSONArray>();
		inlayHints = new HashMap<File, List<TCInlayHint>>();
	}
	
	@Override
	public String getName()
	{
		return "TC";
	}
	
	@Override
	public int getPriority()
	{
		return TC_PRIORITY;
	}

	@Override
	public void init()
	{
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckTypeEvent.class, this);
		eventhub.register(CodeLensEvent.class, this);
		eventhub.register(InlayHintEvent.class, this);
	}

	@Override
	public RPCMessageList handleEvent(LSPEvent event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			preCheck((CheckPrepareEvent)event);
			return new RPCMessageList();
		}
		else if (event instanceof CheckTypeEvent)
		{
			ASTPlugin ast = registry.getPlugin("AST");
			checkLoadedFiles(ast.getAST(), (CheckTypeEvent)event);
			return warningsToHints();
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

	/**
	 * Go through the list of TC warnings and convert some into inlay hints.
	 */
	private RPCMessageList warningsToHints()
	{
		Map<File, Set<VDMMessage>> messages = messagehub.getPluginMessages(this);
		RPCMessageList result = new RPCMessageList();
		boolean refresh = false;

		for (File file: messages.keySet())
		{
			Iterator<VDMMessage> iter = messages.get(file).iterator();

			while (iter.hasNext())
			{
				VDMMessage msg = iter.next();

				if (msg.number == 5500)		// <inlayhint class>, <argument string>
				{
					String[] parts = msg.message.split("\\s*,\\s*");
					String classname = "workspace.inlays." + parts[0];
					String argument = parts[1];

					try
					{
						Class<?> clazz = Class.forName(classname);
						Constructor<?> ctor = clazz.getConstructor(LexLocation.class, String.class);
						TCInlayHint inlay = (TCInlayHint)ctor.newInstance(msg.location, argument);

						addInlayHint(msg.location.file, inlay);
						iter.remove();
						refresh = true;
						break;
					}
					catch (Exception e)
					{
						Diag.warning("Malformed 5500 warning raised exception: ", e.getMessage());
					}
				}
			}
		}

		if (refresh)	// ie. we added an inlay hint
		{
			LSPPlugin lsp = registry.getPlugin("LSP");

			if (lsp.hasClientCapability("workspace.inlayHint.refreshSupport"))
			{
				result.add(RPCRequest.create("workspace/inlayHint/refresh", null));
			}
		}

		return result;
	}

	protected void preCheck(CheckPrepareEvent ev)
	{
		messagehub.clearPluginMessages(this);
		codeLenses.clear();
		inlayHints.clear();
	}
	
	/**
	 * Event handling above. Supporting methods below.
	 * 
	 * Note that TCCodeLenses are factories which support one type of lens each. So here,
	 * we return one TCCodeLens for each *type* of TC lens. Compare this with PO, where
	 * each POCodeLens supports one *instance* of a type of PO lens.
	 */
	protected List<TCCodeLens> getTCCodeLenses(boolean dirty)
	{
		List<TCCodeLens> lenses = new Vector<TCCodeLens>();
		
		if (!dirty)
		{
			lenses.add(new TCLaunchDebugLens());
		}
		
		return lenses;
	}

		
	public void addInlayHint(File file, TCInlayHint hint)
	{
		List<TCInlayHint> list = inlayHints.get(file);
		
		if (list == null)
		{
			list = new Vector<TCInlayHint>();
			inlayHints.put(file, list);
		}
		
		list.add(hint);
	}

	private JSONArray getInlayHints(File file, JSONObject range)
	{
		JSONArray results = new JSONArray();
		ASTPlugin ast = registry.getPlugin("AST");
		boolean dirty = ast.isDirty();

		if (inlayHints.containsKey(file))
		{
			for (InlayHint hint: inlayHints.get(file))
			{
				if (!dirty || hint.whenDirty())
				{
					results.add(hint.getInlayHint());
				}
			}
		}
		
		return results;
	}
	
	public TCExpression checkExpression(ASTExpression ast, Environment env) throws Exception
	{
		TCExpression tc = ClassMapper.getInstance(TCNode.MAPPINGS).convertLocal(ast);
		tc.typeCheck(env, null, NameScope.NAMESANDSTATE, null);
		return tc;
	}

	abstract protected JSONArray getCodeLenses(File file);

	abstract public <T extends Mappable> T getTC();

	abstract public Environment getGlobalEnvironment();
	
	abstract public <T extends Mappable> void checkLoadedFiles(T ast, CheckTypeEvent event) throws Exception;

	abstract public JSONArray documentSymbols(File file);

	abstract public TCDefinition findDefinition(File file, long zline, long zcol);

	abstract public TCDefinitionList lookupDefinition(String startsWith);
	
	abstract public void saveDependencies(File saveUri) throws IOException;

	abstract public TCClassList getTypeHierarchy(String classname, boolean subtypes);

	/**
	 * Common methods for hierarchical outlines.
	 */
	protected JSONObject documentSymbolsTop(TCDefinition top)
	{
		JSONObject result = null;
		TCDefinitionList alldefs = top.getDefinitions();
		
		alldefs.sort(new Comparator<TCDefinition>()
		{
			@Override
			public int compare(TCDefinition d1, TCDefinition d2)
			{
				// Order definitions by the location of their names. All "getDefinitions"
				// include non-null names (ie. values do).
				return d1.name.getLocation().compareTo(d2.name.getLocation());
			}
		});
		
		if (!alldefs.isEmpty())
		{
			Iterator<TCDefinition> iter = alldefs.iterator();
			JSONArray children = new JSONArray();
			
			if (top instanceof TCStateDefinition)
			{
				result = messages.documentSymbol(
						top.name.getName(),
						"",
						SymbolKind.Struct,
						top.location,
						top.location);

				iter.next();	// Ignore state record
				iter.next();	// Ignore "old" state record
				// iter fields follow
			}
			else if (top instanceof TCValueDefinition && alldefs.size() > 1)
			{
				TCValueDefinition vdef = (TCValueDefinition)top;
				
				result = messages.documentSymbol(
						vdef.pattern.toString(),
						"",
						SymbolKind.Struct,
						vdef.pattern.location,
						vdef.pattern.location);
			}
			else if (top instanceof TCThreadDefinition)
			{
				return messages.documentSymbol(
					"thread",
					"()",
					SymbolKind.kindOf(top),
					top.location,
					top.location);
			}
			else if (top instanceof TCPerSyncDefinition ||
					 top instanceof TCMutexSyncDefinition)
			{
				result = messages.documentSymbol(
						top.toString(),
						"",
						SymbolKind.Enum,
						top.location,
						top.location);
				
				iter.next();	// Ignore def
			}
			else if (top instanceof TCTypeDefinition)
			{
				TCType type = top.getType();
				String detail = null;
				
				if (type instanceof TCNamedType)
				{
					TCNamedType ntype = (TCNamedType)type;
					detail = ntype.type.toString();
				}
				else if (type instanceof TCRecordType)
				{
					TCRecordType rtype = (TCRecordType)type;
					detail = "";
					
					for (TCField field: rtype.fields)
					{
						children.add(messages.documentSymbol(
							field.tag,
							field.type.toString(),
							SymbolKind.Field,
							field.tagname.getLocation(),
							field.tagname.getLocation()));
					}
				}
				
				result = messages.documentSymbol(
					top.name.getName(),
					detail,
					SymbolKind.kindOf(top),
					top.name.getLocation(),
					top.name.getLocation());
				
				iter.next();
			}
			else
			{
				TCDefinition head = iter.next();	// 1st def is usually the root
				result = documentSymbolsDef(head);
			}
			
			if (iter.hasNext())
			{
				while (iter.hasNext())
				{
					TCDefinition def = iter.next();
					
					if (def.name != null && !def.name.isOld())
					{
						children.add(documentSymbolsDef(def));
					}
				}
			}
			
			if (!children.isEmpty())
			{
				result.put("children", children);
			}
		}
		
		return result;
	}

	private JSONObject documentSymbolsDef(TCDefinition def)
	{
		return messages.documentSymbol(
			def.name.getName(),
			def.getType().toString(),
			SymbolKind.kindOf(def),
			def.name.getLocation(),
			def.name.getLocation());
	}
}
