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
import java.util.List;
import java.util.Vector;

import json.JSONArray;
import json.JSONObject;
import lsp.LSPMessageUtils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.lenses.CodeLens;

abstract public class AnalysisPlugin
{
	protected final LSPMessageUtils messages;
	
	public AnalysisPlugin()
	{
		messages = new LSPMessageUtils();
	}
	
	protected RPCMessageList errorResult()
	{
		return new RPCMessageList(null, RPCErrors.InternalError, "?");
	}

	abstract public String getName();
	
	abstract public void init();
	
	/**
	 * External plugins claim to support specific LSP messages. This method
	 * identifies whether the plugin supports the name passed.
	 */
	public boolean supportsMethod(String method)
	{
		return false;
	}

	/**
	 * External plugins override this method to implement their functionality.
	 * @param request
	 * @return responses
	 */
	public RPCMessageList analyse(RPCRequest request)
	{
		return new RPCMessageList(request, RPCErrors.InternalError, "Plugin does not support analysis");
	}

	/**
	 * All plugins can register experimental options that are sent back to the Client
	 * in the experimental section of the initialize response.
	 * @param standard 
	 */
	public JSONObject getExperimentalOptions(JSONObject standard)
	{
		return new JSONObject();
	}
	
	/**
	 * Plugins can define code lenses here.
	 * @param dirty 
	 */
	protected List<CodeLens> getCodeLenses(boolean dirty)
	{
		return new Vector<CodeLens>();
	}

	/**
	 * Plugins can apply their code lenses by overriding this method.
	 */
	public JSONArray applyCodeLenses(File file, boolean dirty)
	{
		return new JSONArray();
	}
}
