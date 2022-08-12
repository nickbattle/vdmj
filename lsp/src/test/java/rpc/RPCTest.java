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

package rpc;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Test;

import json.JSONArray;
import json.JSONObject;
import json.JSONReader;
import json.JSONWriter;
import junit.framework.TestCase;

public class RPCTest extends TestCase
{
	@Test
	public void test1() throws IOException
	{
		String json = "{\"jsonrpc\": \"2.0\", \"method\": \"subtract\", \"params\": {\"subtrahend\": 23, \"minuend\": 42}, \"id\": 3}";

		StringReader ireader = new StringReader(json);
		JSONReader reader = new JSONReader(ireader);
		JSONObject map = reader.readObject();
		RPCDispatcher d = new RPCDispatcher();

		d.register(new RPCHandler()
		{
			@Override
			public RPCMessageList request(RPCRequest request) throws IOException
			{
				JSONObject params = request.get("params");
				Long minuend = params.get("minuend");
				Long subtrahend = params.get("subtrahend");
				return new RPCMessageList(RPCResponse.result(request, minuend.longValue() - subtrahend.longValue()));
			}

			@Override
			public void response(RPCResponse message)
			{
			}
		}, "subtract");
		
		RPCMessageList responses = d.dispatch(RPCRequest.create(map));
		StringWriter out = new StringWriter();
		JSONWriter writer = new JSONWriter(new PrintWriter(out));
		writer.writeObject(responses.get(0));
		writer.flush();
		
		System.out.println(out.toString());
		assertEquals(Long.valueOf(19), responses.get(0).get("result"));
	}
	
	@Test
	public void test2() throws IOException
	{
		String json = "{\"jsonrpc\": \"2.0\", \"method\": \"subtract\", \"params\": [ 23, 42 ], \"id\": 3}";

		StringReader ireader = new StringReader(json);
		JSONReader reader = new JSONReader(ireader);
		JSONObject map = reader.readObject();
		RPCDispatcher d = new RPCDispatcher();

		d.register(new RPCHandler()
		{
			@Override
			public RPCMessageList request(RPCRequest request) throws IOException
			{
				JSONArray params = request.get("params");
				Long minuend = params.index(0);
				Long subtrahend = params.index(1);
				return new RPCMessageList(RPCResponse.result(request, minuend.longValue() - subtrahend.longValue()));
			}

			@Override
			public void response(RPCResponse message)
			{
			}
		}, "subtract");
		
		RPCMessageList responses = d.dispatch(RPCRequest.create(map));
		StringWriter out = new StringWriter();
		JSONWriter writer = new JSONWriter(new PrintWriter(out));
		writer.writeObject(responses.get(0));
		writer.flush();
		
		System.out.println(out.toString());
		assertEquals(Long.valueOf(-19), responses.get(0).get("result"));
	}
	
	@Test
	public void testUnknownMethod() throws IOException
	{
		String json = "{\"jsonrpc\": \"2.0\", \"method\": \"wibble\", \"params\": {\"subtrahend\": 23, \"minuend\": 42}, \"id\": 3}";

		StringReader ireader = new StringReader(json);
		JSONReader reader = new JSONReader(ireader);
		JSONObject map = reader.readObject();
		RPCDispatcher d = new RPCDispatcher();
		RPCMessageList responses = d.dispatch(RPCRequest.create(map));
		StringWriter out = new StringWriter();
		JSONWriter writer = new JSONWriter(new PrintWriter(out));
		writer.writeObject(responses.get(0));
		writer.flush();
		
		System.out.println(out.toString());
		JSONObject error = responses.get(0).get("error");
		assertEquals(error.get("code"), Long.valueOf(-32601));
	}
}
