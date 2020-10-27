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

import com.fujitsu.vdmj.Settings;

import json.JSONArray;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.plugins.POPlugin;
import workspace.plugins.TCPlugin;

public class LSPXWorkspaceManager
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
				return INSTANCE;
				
			case VDM_PP:
				if (INSTANCE == null)
				{
					INSTANCE = new LSPXWorkspaceManagerPR();
				}
				return INSTANCE;
				
			case VDM_RT:
				if (INSTANCE == null)
				{
					INSTANCE = new LSPXWorkspaceManagerRT();
				}
				return INSTANCE;
				
			default:
				throw new RuntimeException("Unsupported dialect: " + Settings.dialect);
		}
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
}
