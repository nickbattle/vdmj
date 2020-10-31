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
import java.util.Map;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.traces.TraceReductionType;

import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.plugins.CTPlugin;
import workspace.plugins.INPlugin;
import workspace.plugins.POPlugin;
import workspace.plugins.TCPlugin;

abstract public class LSPXWorkspaceManager
{
	private static LSPXWorkspaceManager INSTANCE = null;
	protected final PluginRegistry registry;
	
	protected LSPXWorkspaceManager()
	{
		this.registry = PluginRegistry.getInstance();
	}

	public static synchronized LSPXWorkspaceManager getInstance()
	{
		switch (Settings.dialect)
		{
			case VDM_SL:
				if (INSTANCE == null)
				{
					INSTANCE = new LSPXWorkspaceManagerSL();
				}
				break;
				
			case VDM_PP:
				if (INSTANCE == null)
				{
					INSTANCE = new LSPXWorkspaceManagerPP();
				}
				break;
				
			case VDM_RT:
				if (INSTANCE == null)
				{
					INSTANCE = new LSPXWorkspaceManagerRT();
				}
				break;
				
			default:
				throw new RuntimeException("Unsupported dialect: " + Settings.dialect);
		}

		return INSTANCE;
	}
	
	/**
	 * This is only used by unit testing.
	 */
	public static void reset()
	{
		if (INSTANCE != null)
		{
			INSTANCE = null;
		}
	}
	
	/**
	 * LSPX extensions...
	 */

	public RPCMessageList pogGenerate(RPCRequest request, File file)
	{
		TCPlugin tc = registry.getPlugin("TC");
		
		if (!tc.getErrs().isEmpty())	// No type clean tree
		{
			return new RPCMessageList(request, RPCErrors.InternalError, "Type checking errors found");
		}
		
		try
		{
			POPlugin po = registry.getPlugin("PO");
	
			if (po.getPO() == null)
			{
				po.checkLoadedFiles(tc.getTC());
			}
			
			JSONArray results = po.getObligations(file);
			return new RPCMessageList(request, results);
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public RPCMessageList ctTraces(RPCRequest request, File project)
	{
		TCPlugin tc = registry.getPlugin("TC");
		
		if (!tc.getErrs().isEmpty())	// No type clean tree
		{
			return new RPCMessageList(request, RPCErrors.InternalError, "Type checking errors found");
		}
		
		try
		{
			CTPlugin ct = registry.getPlugin("CT");
			INPlugin in = registry.getPlugin("IN");
	
			if (ct.getCT() == null)
			{
				ct.checkLoadedFiles(in.getIN());
			}
			
			Map<String, TCNameList> nameMap = ct.getTraceNames();
			JSONArray results = new JSONArray();
			
			for (String module: nameMap.keySet())
			{
				JSONArray array = new JSONArray();
				
				for (TCNameToken name: nameMap.get(module))
				{
					array.add(new JSONObject(
						"name",		name.getExplicit(true).toString(),
						"location",	Utils.lexLocationToLocation(name.getLocation())));
				}
				
				results.add(new JSONObject("name", module, "traces", array));
			}
			
			return new RPCMessageList(request, results);
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public RPCMessageList ctGenerate(RPCRequest request, String name)
	{
		TCPlugin tc = registry.getPlugin("TC");
		
		if (!tc.getErrs().isEmpty())	// No type clean tree
		{
			return new RPCMessageList(request, RPCErrors.InternalError, "Type checking errors found");
		}
		
		try
		{
			CTPlugin ct = registry.getPlugin("CT");
			INPlugin in = registry.getPlugin("IN");
	
			if (ct.getCT() == null)
			{
				ct.checkLoadedFiles(in.getIN());
			}
			
			TCNameToken tracename = stringToName(name);
			int count = ct.generate(tracename);
			return new RPCMessageList(request, new JSONObject("numberOfTests", count));
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public RPCMessageList ctExecute(RPCRequest request, String name,
			Object progressToken, TraceReductionType rType, float subset, long seed, int start, int end)
	{
		TCPlugin tc = registry.getPlugin("TC");
		
		if (!tc.getErrs().isEmpty())	// No type clean tree
		{
			return new RPCMessageList(request, RPCErrors.InternalError, "Type checking errors found");
		}
		
		try
		{
			CTPlugin ct = registry.getPlugin("CT");
			ct.setFilter(rType, subset, seed);
			JSONArray firstBatch = ct.execute(progressToken, start, end);
			return new RPCMessageList(request, firstBatch);
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
	
	/**
	 * This is useful in unit tests to wait for the TraceExecution thread to complete
	 * before doing more tests.
	 */
	public void waitForTraceComplete()
	{
		try
		{
			CTPlugin ct = registry.getPlugin("CT");
			while(!ct.completed());
		}
		catch (Exception e)
		{
			Log.error(e);
		}
	}
	
	private TCNameToken stringToName(String name) throws Exception
	{
		LexTokenReader ltr = new LexTokenReader(name, Dialect.VDM_SL);
		LexToken token = ltr.nextToken();
		ltr.close();

		if (token.is(Token.NAME))
		{
			return new TCNameToken((LexNameToken) token);
		}
		else
		{
			throw new Exception("Name is not fully qualified: " + name);
		}
	}
}
