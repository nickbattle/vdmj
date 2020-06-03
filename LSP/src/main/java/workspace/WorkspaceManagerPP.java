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
import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import lsp.textdocument.SymbolKind;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import vdmj.LSPDefinitionFinder;
import vdmj.LSPDefinitionFinder.Found;

public class WorkspaceManagerPP extends WorkspaceManager
{
	private ASTClassList astClassList = null;
	private TCClassList tcClassList = null;
	private INClassList inClassList = null;

	public WorkspaceManagerPP()
	{
		Settings.dialect = Dialect.VDM_PP;
	}

	@Override
	protected List<VDMMessage> parseFile(File file)
	{
		List<VDMMessage> errs = new Vector<VDMMessage>();
		StringBuilder buffer = projectFiles.get(file);
		
		LexTokenReader ltr = new LexTokenReader(buffer.toString(),
				Settings.dialect, file, Charset.defaultCharset().displayName());
		ClassReader cr = new ClassReader(ltr);
		cr.readClasses();
		
		if (cr.getErrorCount() > 0)
		{
			errs.addAll(cr.getErrors());
		}
		
		if (cr.getWarningCount() > 0)
		{
			errs.addAll(cr.getWarnings());
		}

		Log.dump(errs);
		return errs;
	}
	
	protected ASTClassList extras()
	{
		return new ASTClassList();		// Overridden in RT to add CPU and BUS
	}

	@Override
	protected RPCMessageList checkLoadedFiles() throws Exception
	{
		astClassList = new ASTClassList();
		List<VDMMessage> errs = new Vector<VDMMessage>();
		List<VDMMessage> warns = new Vector<VDMMessage>();
		
		for (Entry<File, StringBuilder> entry: projectFiles.entrySet())
		{
			LexTokenReader ltr = new LexTokenReader(entry.getValue().toString(),
					Settings.dialect, entry.getKey(), Charset.defaultCharset().displayName());
			ClassReader cr = new ClassReader(ltr);
			astClassList.addAll(cr.readClasses());
			
			if (cr.getErrorCount() > 0)
			{
				errs.addAll(cr.getErrors());
			}
			
			if (cr.getWarningCount() > 0)
			{
				warns.addAll(cr.getWarnings());
			}
		}
		
		astClassList.addAll(extras());
		
		if (errs.isEmpty())
		{
			tcClassList = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(astClassList);
			TypeChecker tc = new ClassTypeChecker(tcClassList);
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
			tcClassList = null;
		}
		
		if (errs.isEmpty())
		{
			inClassList = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(tcClassList);
		}
		else
		{
			Log.error("Type checking errors found");
			Log.dump(errs);
			Log.dump(warns);
			inClassList = null;
		}
		
		errs.addAll(warns);
		return diagnosticResponses(errs, null);
	}

	@Override
	protected TCNode findLocation(File file, int zline, int zcol)
	{
		if (tcClassList != null && !tcClassList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			Found found = finder.findLocation(tcClassList, file, zline + 1, zcol + 1);
			
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
		if (tcClassList != null && !tcClassList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			return finder.findDefinition(tcClassList, file, zline + 1, zcol + 1);		// Convert from zero-relative
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
		
		for (TCClassDefinition cdef: tcClassList)
		{
			if (cdef.name.getName().startsWith(startsWith))
			{
				results.add(cdef);	// Add classes as well
			}
			
			for (TCDefinition def: cdef.definitions)
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
		return Dialect.VDM_PP.getFilter();
	}
	
	@Override
	protected String[] getFilenameFilters()
	{
		return new String[] { "**/*.vpp", "**/*.vdmpp" }; 
	}

	@Override
	public RPCMessageList documentSymbols(RPCRequest request, File file)
	{
		JSONArray results = new JSONArray();
		
		if (tcClassList != null)	// May be syntax errors
		{
			for (TCClassDefinition clazz: tcClassList)
			{
				if (clazz.name.getLocation().file.equals(file))
				{
					results.add(symbolInformation(clazz.name.toString(), clazz.name.getLocation(), SymbolKind.Class, null));

					for (TCDefinition def: clazz.definitions)
					{
						for (TCDefinition indef: def.getDefinitions())
						{
							results.add(symbolInformation(indef.name.getName() + ":" + indef.getType(), indef.location, SymbolKind.kindOf(indef), indef.location.module));
						}
					}
				}
			}
		}
		else if (astClassList != null)		// Try AST instead
		{
			for (ASTClassDefinition clazz: astClassList)
			{
				if (clazz.name.location.file.equals(file))
				{
					results.add(symbolInformation(clazz.name.toString(), clazz.location, SymbolKind.Class, null));

					for (ASTDefinition def: clazz.definitions)
					{
						if (def.name != null)
						{
							results.add(symbolInformation(def.name.name, def.name.location,
									SymbolKind.kindOf(def), def.location.module));
						}
					}
				}
			}
		}
		
		return new RPCMessageList(request, results);
	}

	@Override
	public ClassInterpreter getInterpreter()
	{
		if (interpreter == null)
		{
			try
			{
				interpreter = new ClassInterpreter(inClassList, tcClassList);
			}
			catch (Exception e)
			{
				Log.error(e);
				interpreter = null;
			}
		}
		
		return (ClassInterpreter) interpreter;
	}

	@Override
	protected boolean canExecute()
	{
		return getInterpreter() != null;	// inClassList != null;
	}
	
	@Override
	protected boolean hasChanged()
	{
		return getInterpreter() != null && getInterpreter().getIN() != inClassList;
	}

	@Override
	public DAPMessageList threads(DAPRequest request)
	{
		return new DAPMessageList(request, new JSONObject("threads", new JSONArray()));	// empty?
	}
}
