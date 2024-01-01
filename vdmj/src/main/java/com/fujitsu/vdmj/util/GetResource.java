/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package com.fujitsu.vdmj.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;

/**
 * A utility class to find and load a file from the classpath into a
 * temporary or given location, or read from resource files.
 */
public class GetResource
{
	public static boolean find(File file)
	{
		return GetResource.class.getResource("/" + file.getName()) != null;
	}
	
	public static File load(File file) throws IOException
	{
		File temp = File.createTempFile("tmp", file.getName());
		temp.deleteOnExit();
		return load(file, temp);
	}
	
	public static File load(File file, File dest) throws IOException
	{
		/**
		 * Note: we assume libraries are UTF8 encoded, but write them as the
		 * local file encoding for the session.
		 */
		InputStream in = GetResource.class.getResourceAsStream("/" + file.getName());
		InputStreamReader isr = new InputStreamReader(in, "UTF8");
		OutputStream out = new FileOutputStream(dest);
		OutputStreamWriter osr = new OutputStreamWriter(out, Settings.filecharset);
		
		char[] buf = new char[8192];
	    int length;

	    while ((length = isr.read(buf, 0, 8192)) > 0)
	    {
	        osr.write(buf, 0, length);
	    }
	    
		isr.close();
		osr.close();
		
		return dest;
	}
	
	public static List<String> readResource(String resourceName) throws Exception
	{
		return readResource(resourceName, resourceName);
	}
	
	public static List<String> readResource(String resourceName, String propertyName) throws Exception
	{
		String property = System.getProperty(propertyName);
		List<String> results = new Vector<String>();
		
		if (property != null)	// Overrides the resources
		{
			if (!property.isEmpty())
			{
				results.addAll(Arrays.asList(property.split("\\s*[,;]\\s*")));
			}
			
			return results;
		}
		else
		{
			
			Enumeration<URL> urls = GetResource.class.getClassLoader().getResources(resourceName);
	
			while (urls.hasMoreElements())
			{
				URL url = urls.nextElement();
				BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
				
				for (String line = br.readLine(); line != null; line = br.readLine())
				{
					results.add(line);
				}
				
				br.close();
			}
			
			return results;
		}
	}
}
