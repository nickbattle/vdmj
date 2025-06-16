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

import java.io.IOException;

import json.JSONObject;

public class RPCRequest extends JSONObject
{
	private static final long serialVersionUID = 1L;
	private static long nextId = 1;
	
	private RPCRequest(String method, Object params)
	{
		put("jsonrpc", "2.0");
		put("method", method);
		put("params", params);
	}
	
	private RPCRequest(JSONObject request) throws IOException
	{
		String version = request.get("jsonrpc");
		
		if (!"2.0".equals(version))
		{
			throw new IOException("Unsupported JSON RPC version - expecting 2.0");
		}
		else
		{
			putAll(request);
		}
	}

	/**
	 * Public methods below used to create messages.
	 */
	
	public static RPCRequest create(JSONObject request) throws IOException
	{
		return new RPCRequest(request);
	}
	
	public static RPCRequest notification(String method, Object params)
	{
		return new RPCRequest(method, params);	// Note: no id field.
	}
	
	public synchronized static RPCRequest create(String method, Object params)
	{
		RPCRequest request = new RPCRequest(method, params);
		request.put("id", nextId++);
		return request;
	}
	
	public synchronized static RPCRequest create(Long id, String method, Object params)
	{
		nextId = id;
		RPCRequest request = new RPCRequest(method, params);
		request.put("id", nextId++);
		return request;
	}
	
	public String getMethod()
	{
		return get("method");
	}
}
