/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package com.fujitsu.vdmjunit;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.plugins.Lifecycle;
import com.fujitsu.vdmj.plugins.events.AbstractCheckFilesEvent;
import com.fujitsu.vdmj.util.GetResource;

public class VDMJUnitLifecycle extends Lifecycle
{
	protected List<VDMError> errors = new Vector<VDMError>();
	protected List<VDMWarning> warnings = new Vector<VDMWarning>();

	public VDMJUnitLifecycle(String... args)
	{
		super(args);
	}
	
	@Override
	protected void loadPlugins()
	{
		super.loadPlugins();
	}
	
	@Override
	protected void processArgs()
	{
		super.processArgs();
	}
	
	protected void findFiles(Charset charset, String... filenames) throws Exception
	{
		Settings.filecharset = charset;
		files = new Vector<File>(filenames.length);
		
		for (String filename: filenames)
		{
			URL url = getClass().getResource("/" + filename);
			
			if (url == null)
			{
				throw new FileNotFoundException(filename);
			}
			
			File file = null;
			
			if (url.getProtocol().equals("jar"))
			{
				file = GetResource.load(new File("/" + filename));
			}
			else
			{
				file = new File(url.toURI());
			}
			
			if (file.isDirectory())
			{
				for (File subfile: file.listFiles(Settings.dialect.getFilter()))
				{
					if (subfile.isFile())
					{
						files.add(subfile);
					}
				}
			}
			else
			{
				files.add(file);
			}
		}
	}
	
	@Override
	protected boolean checkAndInitFiles()
	{
		return super.checkAndInitFiles();
	}
	
	@Override
	protected boolean report(List<VDMMessage> messages, AbstractCheckFilesEvent event)
	{
		for (VDMMessage m: messages)
		{
			if (m instanceof VDMError && !errors.contains(m))
			{
				errors.add((VDMError)m);
			}
			else if (m instanceof VDMWarning && ! warnings.contains(m))
			{
				warnings.add((VDMWarning)m);
			}
		}

		return super.report(messages, event);
	}
}
