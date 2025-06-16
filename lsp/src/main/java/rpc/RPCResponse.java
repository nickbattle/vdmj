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

package rpc;

import json.JSONObject;

public class RPCResponse extends JSONObject
{
	private static final long serialVersionUID = 1L;

	private RPCResponse()
	{
		put("jsonrpc", "2.0");
	}
	
	private RPCResponse(RPCRequest request, Object result)
	{
		this();
		put("result", result);
		put("id", request.get("id"));
	}

	private RPCResponse(RPCRequest request, RPCErrors error, String message, Object data)
	{
		this();
		JSONObject params = new JSONObject("code", error.getValue());
		if (message != null) params.put("message", message);
		if (data != null) params.put("data", data);
		put("error", params);
		put("id", request.get("id"));
	}

	private RPCResponse(JSONObject result)
	{
		this();
		putAll(result);
	}
	
	/**
	 * Public methods below used to create messages.
	 */
	
	public static RPCResponse create(JSONObject result)
	{
		return new RPCResponse(result);
	}
	
	public static RPCResponse result(RPCRequest request, Object result)
	{
		return new RPCResponse(request, result);
	}

	public static RPCResponse result(RPCRequest request)
	{
		return new RPCResponse(request, null);
	}

	public static RPCResponse error(RPCRequest request, RPCErrors error, String message)
	{
		return new RPCResponse(request, error, message, null);
	}
	
	public static RPCResponse error(RPCRequest request, RPCErrors error, String message, Object data)
	{
		return new RPCResponse(request, error, message, data);
	}
	
	public static RPCResponse error(RPCErrors error, String message)
	{
		RPCResponse response = new RPCResponse();
		response.put("error", new JSONObject("code", error.getValue(), "message", message));
		return response;
	}
	
	public boolean isError()
	{
		return get("error") != null;
	}
	
	public String getError()
	{
		JSONObject error = get("error");
		
		if (error != null)
		{
			return error.get("message");
		}
		
		return "success";
	}
}
