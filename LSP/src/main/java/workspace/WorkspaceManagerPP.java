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
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
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
import rpc.RPCResponse;
import vdmj.LSPDefinitionFinder;

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
				Dialect.VDM_PP, file, Charset.defaultCharset().displayName());
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

	@Override
	protected RPCMessageList checkLoadedFiles() throws Exception
	{
		astClassList = new ASTClassList();
		List<VDMMessage> errs = new Vector<VDMMessage>();
		List<VDMMessage> warns = new Vector<VDMMessage>();
		
		for (Entry<File, StringBuilder> entry: projectFiles.entrySet())
		{
			LexTokenReader ltr = new LexTokenReader(entry.getValue().toString(),
					Dialect.VDM_PP, entry.getKey(), Charset.defaultCharset().displayName());
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
		}
		
		errs.addAll(warns);
		return diagnosticResponses(errs, null);
	}

	@Override
	public RPCMessageList findDefinition(RPCRequest request, File file, int line, int col)
	{
		if (!tcClassList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			TCDefinition def = finder.find(tcClassList, file, line + 1, col + 1);
			
			if (def == null)
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Definition not found");
			}
			else
			{
				URI defuri = def.location.file.toURI();
				
				return new RPCMessageList(request,
						new JSONArray(
							new JSONObject(
								"targetUri", defuri.toString(),
								"targetRange", Utils.lexLocationToRange(def.location),
								"targetSelectionRange", Utils.lexLocationToPoint(def.location))));
			}
		}
		else
		{
			return new RPCMessageList(new RPCResponse(request, "Specification has errors"));
		}
	}

	@Override
	protected FilenameFilter getFilenameFilter()
	{
		return Dialect.VDM_PP.getFilter();
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
						results.add(symbolInformation(def.name.name, def.name.location, SymbolKind.kindOf(def), def.location.module));
					}
				}
			}
		}
		
		return new RPCMessageList(request, results);
	}

	@Override
	public ClassInterpreter getInterpreter() throws Exception
	{
		if (interpreter == null)
		{
			interpreter = new ClassInterpreter(inClassList, tcClassList);
		}
		
		return (ClassInterpreter) interpreter;
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
			for (ASTClassDefinition m: astClassList)
			{
				if (m.name.name.equals(expression))
				{
					interpreter.setDefaultName(expression);
					DAPMessageList responses = new DAPMessageList(request);
					responses.add(prompt());
					return responses;
				}
			}
			
			return super.evaluate(request, expression, context);
		}
		catch (Exception e)
		{
			DAPMessageList responses = new DAPMessageList(request, e);
			responses.add(prompt());
			return responses;
		}
	}
}
