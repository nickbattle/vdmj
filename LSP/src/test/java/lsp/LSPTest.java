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

package lsp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;

import org.junit.Test;

import json.JSONObject;

public class LSPTest
{
	@Test
	public void testSimple() throws Exception
	{
		StringBuilder buffer = new StringBuilder("0123456789");
		int start = Utils.findPosition(buffer, new JSONObject("line", 0L, "character", 2L));
		int end = Utils.findPosition(buffer, new JSONObject("line", 0L, "character", 4L));
		buffer.replace(start, end, "hello");
		assertEquals("01hello456789", buffer.toString());
	}

	@Test
	public void testNewline() throws Exception
	{
		StringBuilder buffer = new StringBuilder("01234\n0123456789\n");
		int start = Utils.findPosition(buffer, new JSONObject("line", 1L, "character", 2L));
		int end = Utils.findPosition(buffer, new JSONObject("line", 1L, "character", 4L));
		buffer.replace(start, end, "hello");
		assertEquals("01234\n01hello456789\n", buffer.toString());
	}

	@Test
	public void testDelete() throws Exception
	{
		StringBuilder buffer = new StringBuilder("0123456789");
		int start = Utils.findPosition(buffer, new JSONObject("line", 0L, "character", 2L));
		int end = Utils.findPosition(buffer, new JSONObject("line", 0L, "character", 4L));
		buffer.replace(start, end, "");
		assertEquals("01456789", buffer.toString());
	}

	@Test
	public void testURIs()
	{
		File file = null;
		URI uri = null;
		
		try
		{
			uri = new URI("file:///c%3A/Users/jonas/repos/vdm-lsp/Code/VDM-LSP_Java/vdm/AlarmSL");
			file = new File(uri);
			System.out.println(file.toURI());
			System.out.println(file.getCanonicalPath());
			
			if (File.separatorChar == '/')
			{
				uri = new URI("file", "", file.getCanonicalPath(), null, null);
			}
			else
			{
				uri = new URI("file", "", "\\" + file.getCanonicalPath(), null, null);
			}
			
			System.out.println(uri.toString());
		}
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
	}
}
