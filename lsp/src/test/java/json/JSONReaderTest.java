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

package json;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import org.junit.Test;

public class JSONReaderTest extends TestCase
{
	private final Random r = new Random();

	@Test
	public void testRandom() throws IOException
	{
		for (int seed=0; seed < 100; seed++)
		{
			r.setSeed(seed);
			String json = getObject();
			// System.out.println(json);
			StringReader ireader = new StringReader(json);
	
			JSONReader reader = new JSONReader(ireader);
			JSONObject map = reader.readObject();
			
			StringWriter out = new StringWriter();
			JSONWriter writer = new JSONWriter(new PrintWriter(out));
			writer.writeObject(map);
			writer.flush();
			// System.out.println(out.toString());
			// System.out.println("--------");
			
			String original = json.replaceAll(" ", "");
			String after = out.toString().replaceAll(" ", "");
			assertEquals(original, after);
		}
	}
	
	@Test
	public void testQuotedStrings() throws IOException
	{
		JSONReader reader = new JSONReader(new StringReader("{ \"abc\" : \"def\" }"));
		Map<String, Object> map = reader.readObject();
		assertEquals(map.get("abc"), "def");

		reader = new JSONReader(new StringReader("{ \"abc\" : \"\\n\" }"));
		map = reader.readObject();
		assertEquals(map.get("abc"), "\n");

		reader = new JSONReader(new StringReader("{ \"abc\" : \"\\r\" }"));
		map = reader.readObject();
		assertEquals(map.get("abc"), "\r");

		reader = new JSONReader(new StringReader("{ \"abc\" : \"\\t\" }"));
		map = reader.readObject();
		assertEquals(map.get("abc"), "\t");

		reader = new JSONReader(new StringReader("{ \"abc\" : \"\\f\" }"));
		map = reader.readObject();
		assertEquals(map.get("abc"), "\f");

		reader = new JSONReader(new StringReader("{ \"abc\" : \"\\v\" }"));
		map = reader.readObject();
		assertEquals(map.get("abc"), "\u000B");

		reader = new JSONReader(new StringReader("{ \"abc\" : \"\\b\" }"));
		map = reader.readObject();
		assertEquals(map.get("abc"), "\b");

		reader = new JSONReader(new StringReader("{ \"abc\" : \"\\\"\" }"));
		map = reader.readObject();
		assertEquals(map.get("abc"), "\"");

		reader = new JSONReader(new StringReader("{ \"abc\" : \"\\\\\" }"));
		map = reader.readObject();
		assertEquals(map.get("abc"), "\\");

		reader = new JSONReader(new StringReader("{ \"abc\" : \"\\/\" }"));
		map = reader.readObject();
		assertEquals(map.get("abc"), "/");

		reader = new JSONReader(new StringReader("{ \"abc\" : \"\\uABCD\" }"));
		map = reader.readObject();
		assertEquals(map.get("abc"), "\uABCD");
	}
	
	@Test
	public void testNumbers()
	{
		long lvalue = 123;
		int ivalue = 456;
		short svalue = 789;
		
		JSONArray array = new JSONArray(lvalue, ivalue, svalue);
		
		for (Object v: array)
		{
			assertTrue(v instanceof Long);
		}
		
		JSONObject object = new JSONObject("lvalue", lvalue, "ivalue", ivalue, "svalue", svalue);
		
		assertTrue(object.get("lvalue") instanceof Long);
		assertTrue(object.get("ivalue") instanceof Long);
		assertTrue(object.get("svalue") instanceof Long);
	}
	
	
	private String getWS()
	{
		switch (r.nextInt(3))
		{
			case 0: return "";
			case 1: return " ";
			case 2: return "  ";
		}
		
		return "";
	}
	
	private String getString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getWS());
		sb.append('"');
		
		for (int i=0; i<r.nextInt(5)+3; i++)
		{
			switch (r.nextInt(50))
			{
				case 0: sb.append("\\r"); break;
				case 1: sb.append("\\n"); break;
				case 2: sb.append("\\t"); break;
				case 3: sb.append("\\f"); break;
				case 4: sb.append("\\b"); break;
				case 5: sb.append("\\v"); break;
				case 6: sb.append("\\\\"); break;
				case 7: sb.append(" "); break;
				
				default:
					sb.append((char)('a' + r.nextInt(26)));
			}
		}
		
		sb.append('"');
		sb.append(getWS());
		return sb.toString();
	}
	
	private String getArray()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(getWS());
		sb.append("[");
		String sep = "";
		
		for (int f=0; f < r.nextInt(5); f++)
		{
			sb.append(sep);
			sep = ",";
			sb.append(getValue());
		}
		
		sb.append("]");
		sb.append(getWS());
		
		return sb.toString();
	}
	
	private String getObject()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(getWS());
		sb.append("{");
		String sep = "";
		
		for (int f=1; f <= r.nextInt(5)+1; f++)
		{
			sb.append(sep);
			sep = ",";
			sb.append(getString());
			sb.append(":");
			sb.append(getValue());
		}
		
		sb.append("}");
		sb.append(getWS());
		
		return sb.toString();
	}
	
	private String getValue()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getWS());

		switch (r.nextInt(8))
		{
			case 0: sb.append(r.nextLong()); break;
			case 1: sb.append(r.nextInt()); break;
			case 2: sb.append(r.nextDouble() * Math.pow(10, r.nextInt(50) + 1)); break; 
			case 3: sb.append(r.nextBoolean()); break;
			case 4: sb.append(getArray()); break;
			case 5: sb.append(getObject()); break;
			case 6: sb.append(getString()); break;
			case 7: sb.append("null");
		}
		
		sb.append(getWS());
		return sb.toString();
	}
}
