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
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.traces.TraceReductionType;

import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.plugins.ASTPlugin;
import workspace.plugins.CTPlugin;
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
		try
		{
			TCPlugin tc = registry.getPlugin("TC");
			
			if (!tc.getErrs().isEmpty())	// No type clean tree
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Type checking errors found");
			}
			
			POPlugin po = registry.getPlugin("PO");
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
		try
		{
			if (specHasErrors())
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Specification has errors");
			}
			
			DAPWorkspaceManager.getInstance().refreshInterpreter();
			CTPlugin ct = registry.getPlugin("CT");
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
		try
		{
			if (specHasErrors())
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Specification has errors");
			}
			
			CTPlugin ct = registry.getPlugin("CT");
			
			if (ct.isRunning())
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Trace still running");
			}
	
			DAPWorkspaceManager.getInstance().refreshInterpreter();
			TCNameToken tracename = Utils.stringToName(name);
			int count = ct.generate(tracename);
			return new RPCMessageList(request, new JSONObject("numberOfTests", count));
		}
		catch (InterruptedException e)	// generate was cancelled
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.RequestCancelled, e.getMessage());
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public RPCMessageList ctExecute(RPCRequest request, String name,
			Object progressToken, Object workDoneToken,
			TraceReductionType rType, float subset, long seed, long start, long end)
	{
		try
		{
			if (specHasErrors())
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Specification has errors");
			}
			
			CTPlugin ct = registry.getPlugin("CT");
			
			if (ct.isRunning())
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Trace still running");
			}

			DAPWorkspaceManager.getInstance().refreshInterpreter();
			TCNameToken tracename = Utils.stringToName(name);
			JSONArray batch = ct.execute(request, tracename, progressToken, workDoneToken, rType, subset, seed, start, end);
			
			if (batch == null)	// Running in background
			{
				return null;
			}
			else
			{
				return new RPCMessageList(request, batch);
			}
		}
		catch (InterruptedException e)	// execute was cancelled
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.RequestCancelled, e.getMessage());
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
	
	private boolean specHasErrors()
	{
		ASTPlugin ast = registry.getPlugin("AST");
		TCPlugin tc = registry.getPlugin("TC");
		
		return !ast.getErrs().isEmpty() || !tc.getErrs().isEmpty();
	}
}
