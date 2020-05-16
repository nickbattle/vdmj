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

package rpc;

import json.JSONObject;

public class RPCResponse extends JSONObject
{
	private static final long serialVersionUID = 1L;

	private RPCResponse()
	{
		put("jsonrpc", "2.0");
	}
	
	public RPCResponse(RPCRequest request, Object result)
	{
		this();
		put("result", result);
		put("id", request.get("id"));
	}
	
	public RPCResponse(JSONObject result)
	{
		this();
		putAll(result);
	}
	
	public RPCResponse(RPCRequest request, RPCErrors error, String message)
	{
		this();
		put("error", new JSONObject("code", error.getValue(), "message", message));
		put("id", request.get("id"));
	}
	
	public RPCResponse(RPCErrors error, String message)
	{
		this();
		put("error", new JSONObject("code", error.getValue(), "message", message));
	}
}
