/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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

package com.fujitsu.vdmjc.config;

import java.io.InputStream;
import java.util.Properties;

/**
 * The Config class is used to hold global configuration values. The
 * values are read from the dbgp.properties file, and defaults are defined
 * as public statics.
 */
public class Config
{
	/** The maximum number of listener connections. */
	public static int listener_connection_limit = 100;

	/** The VDMJ jar location. */
	public static String vdmj_jar = "";

	/** Extra VDMJ JVM arguments. */
	public static String vdmj_jvm = "";

	/** The DBGP jar location */
	public static String dbgp_jar = "";

	/**
	 * When the class is initialized, which uses the dbgp.properties file, and any System
	 * properties, to set the static fields above.
	 */
	public static void init()
	{
		try
		{
			java.util.Properties local = new java.util.Properties();
			InputStream s = Properties.class.getResourceAsStream("/dbgpc.properties");
			
			if (s != null)
			{
				local.load(s);
				s.close();
			}
			
			listener_connection_limit = get(local, "dbgpc.listener.connection_limit", listener_connection_limit);
			vdmj_jar = get(local, "dbgpc.vdmj_jar", vdmj_jar);
			vdmj_jvm = get(local, "dbgpc.vdmj_jvm", vdmj_jvm);
			dbgp_jar = get(local, "dbgpc.dbgp_jar", dbgp_jar);
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	
	private static int get(java.util.Properties local, String key, int def)
	{
		Integer value = Integer.getInteger(key);
		
		if (value == null)
		{
			if (local.containsKey(key))
			{
				try
				{
					String p = local.getProperty(key);
					value = Integer.parseInt(p);
				}
				catch (NumberFormatException e)
				{
					System.err.println(e.getMessage());
					value = def;
				}
			}
			else
			{
				value = def;
			}
		}
		
		return value;
	}
	
	@SuppressWarnings("unused")
	private static boolean get(java.util.Properties local, String key, boolean def)
	{
		String svalue = System.getProperty(key);
		boolean value = def;
		
		if (svalue == null)
		{
			if (local.containsKey(key))
			{
				value = Boolean.parseBoolean(local.getProperty(key));
			}
		}
		else
		{
			value = Boolean.parseBoolean(svalue);
		}
		
		return value;
	}
	
	private static String get(java.util.Properties local, String key, String def)
	{
		String value = System.getProperty(key);
		
		if (value == null)
		{
			if (local.containsKey(key))
			{
				value = local.getProperty(key);
			}
			else
			{
				value = def;
			}
		}
		
		return value;
	}
}
