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

import java.io.IOException;

import json.JSONObject;

public class RPCRequest extends JSONObject
{
	private static final long serialVersionUID = 1L;
	
	public RPCRequest(JSONObject request) throws IOException
	{
		String version = request.get("jsonrpc");
		
		if (!"2.0".equals(version))
		{
			throw new IOException("Unsupported JSON version - expecting 2.0");
		}
		else
		{
			putAll(request);
		}
	}
	
	public RPCRequest(String method, Object params)
	{
		put("jsonrpc", "2.0");
		put("method", method);
		put("params", params);
	}
	
	public RPCRequest(Long id, String method, Object params)
	{
		this(method, params);
		put("id", id);
	}
	
	public String getMethod()
	{
		return get("method");
	}
}
