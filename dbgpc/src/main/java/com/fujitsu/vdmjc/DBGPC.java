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
 *
 ******************************************************************************/

package com.fujitsu.vdmjc;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.fujitsu.vdmjc.client.CommandLine;
import com.fujitsu.vdmjc.client.Dialect;
import com.fujitsu.vdmjc.client.Release;
import com.fujitsu.vdmjc.config.Config;

public class DBGPC
{
	public static void main(String[] args)
	{
		Config.init();

		try
		{
			Dialect dialect = Dialect.VDM_SL;
			String startLine = null;
			List<File> pathnames = new Vector<File>();

			if (args.length > 0)
			{
				List<String> cmds = new Vector<String>();

				for (Iterator<String> i = Arrays.asList(args).iterator(); i.hasNext();)
				{
					String arg = i.next();
					
					if (arg.equals("-vdmpp"))
					{
						dialect = Dialect.VDM_PP;
					}
					else if (arg.equals("-vdmsl"))
					{
						dialect = Dialect.VDM_SL;
					}
					else if (arg.equals("-vdmrt"))
					{
						dialect = Dialect.VDM_RT;
					}
		    		else if (arg.equals("-v"))
		    		{
		    			String version = getVersion();

		    			if (version == null)
		    			{
		    				System.out.println("Cannot determine jar version");
		    			}
		    			else
		    			{
		    				System.out.println("DBGPC jar version " + version);
		    			}
		    		}
		    		else if (arg.equals("-path"))
		    		{
		    			if (i.hasNext())
		    			{
		       				File path = new File(i.next());
		       				
		       				if (path.isDirectory())
		       				{
		       					pathnames.add(path);
		       				}
		       				else
		       				{
		       					System.out.println(path + " is not a directory");
		       				}
		    			}
		    			else
		    			{
		    				System.out.println("-path option requires a directory");
		    			}
		    		}
					else if (arg.startsWith("-"))
					{
						System.err.println("Usage: DBGPC [-v | -vdmpp | -vdmsl | -vdmrt | -path] [command]");
						System.exit(1);
					}
					else
					{
						cmds.add(arg);
					}
				}

				if (!cmds.isEmpty())
				{
					StringBuilder sb = new StringBuilder();

					for (String file: cmds)
					{
						sb.append(file);
						sb.append(" ");
					}

					startLine = sb.toString();
				}
			}

			System.out.println("Dialect is " + dialect.name() + " " + Release.DEFAULT);
			new CommandLine(dialect, Release.DEFAULT, startLine, pathnames).run();
			System.exit(0);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
	
	private static String getVersion()
	{
		try
		{
			String path = DBGPC.class.getName().replaceAll("\\.", "/");
			URL url = DBGPC.class.getResource("/" + path + ".class");
			JarURLConnection conn = (JarURLConnection)url.openConnection();
		    JarFile jar = conn.getJarFile();
			Manifest mf = jar.getManifest();
			String version = (String)mf.getMainAttributes().get(Attributes.Name.IMPLEMENTATION_VERSION);
			return version;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
