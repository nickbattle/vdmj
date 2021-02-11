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

package vdmj.commands;

import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.fujitsu.vdmj.VDMJ;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import workspace.Log;

public class VersionCommand extends Command
{
	public static final String USAGE = "Usage: version";
	public static final String[] HELP =	{ "version", "version - show the VDMJ version and build" };
	
	public VersionCommand(String line)
	{
		if (!line.trim().equals("version"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		try
		{
			String path = VDMJ.class.getName().replaceAll("\\.", "/");
			URL url = VDMJ.class.getResource("/" + path + ".class");
			JarURLConnection conn = (JarURLConnection)url.openConnection();
		    JarFile jar = conn.getJarFile();
			Manifest mf = jar.getManifest();
			String version = (String)mf.getMainAttributes().get(Attributes.Name.IMPLEMENTATION_VERSION);

			return new DAPMessageList(request,
					new JSONObject("result", "VDMJ version " + version));
		}
		catch (Exception e)
		{
			Log.error(e);
			return new DAPMessageList(request, false, "Cannot determine VDMJ version", null);
		}
	}

	@Override
	public boolean notWhenRunning()
	{
		return false;
	}
}
