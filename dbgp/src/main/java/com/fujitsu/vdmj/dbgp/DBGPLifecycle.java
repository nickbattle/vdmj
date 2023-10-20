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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.dbgp;

import java.io.File;
import java.util.List;

import com.fujitsu.vdmj.plugins.Lifecycle;

public class DBGPLifecycle extends Lifecycle
{
	private final boolean quiet;
	private final boolean warnings;
	
	public DBGPLifecycle(List<File> files, boolean quiet, boolean warnings)
	{
		super(new String[]{});
		
		this.files = files;
		this.quiet = quiet;
		this.warnings = warnings;
	}
	
	@Override
	protected void processArgs()
	{
		if (quiet) argv.add("-q");
		if (!warnings) argv.add("-w");
		
		super.processArgs();
	}
	
	@Override
	public void loadPlugins()
	{
		super.loadPlugins();
	}
	
	@Override
	public boolean checkAndInitFiles()
	{
		return super.checkAndInitFiles();
	}
}
