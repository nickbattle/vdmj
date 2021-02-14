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
package json;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Test;

import junit.framework.TestCase;

public class JSONFileTest extends TestCase
{
	@Test
	public void test()
	{
		// Dummy to allow Maven to run this test
	}
	
	public static void main(String[] args) throws IOException
	{
		FileReader reader = new FileReader(new File(args[0]));
		JSONReader in = new JSONReader(reader);
		JSONObject data = in.readObject();
		
		JSONWriter writer = new JSONWriter(new PrintWriter(System.out));
		writer.writeObject(data);
		writer.flush();
	}
}
