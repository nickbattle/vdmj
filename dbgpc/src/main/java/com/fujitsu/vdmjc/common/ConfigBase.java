/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmjc.common;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

public class ConfigBase
{
	private static Properties props = new Properties();

	public static void init(String resource, Class<?> target) throws Exception
	{
		InputStream fis = null;
		String propertyFile = resource;

		try
		{
    		try
			{
				fis = ConfigBase.class.getResourceAsStream("/" + resource);

				if (fis != null)
				{
					props.load(fis);
				}
			}
    		catch (Exception ex)
    		{
    			throw new Exception(propertyFile + ": " + ex.getMessage());
    		}

    		String pname = "?";
    		String value = "?";

			try
			{
				for (Field f : target.getFields())
				{
					pname = f.getName().replace('_', '.');
					Class<?> type = f.getType();
					value = getProperty(pname, null);
					
					if (value != null)
					{
						if (type == Integer.TYPE)
						{
							f.setInt(target, Integer.parseInt(value));
						}
						else if (type == Boolean.TYPE)
						{
							f.setBoolean(target, Boolean.parseBoolean(value));
						}
						else if (type == String.class)
						{
							f.set(target, value);
						}
						else
						{
							throw new Exception("Cannot process " + pname +
								", Java type " + type + " unsupported");
						}
					}
				}
			}
			catch (Exception ex)
			{
				throw new Exception(propertyFile +
					": (" +	pname + " = " + value + ") " + ex.getMessage());
			}
		}
		finally
		{
			if (fis != null)
			{
				try
				{
					fis.close();
				}
				catch (IOException e)
				{
					// so?
				}
			}
		}
	}

	public static String getProperty(String key, String def)
	{
		String value = System.getProperty(key, def);	// Overrides
		
		if (value == null)
		{
			value = props.getProperty(key, def);
		}

		return value;
	}
}
