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

package workspace.plugins;

import java.io.File;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;

import json.JSONArray;

abstract public class TCPlugin extends AnalysisPlugin
{
	protected static final boolean STRUCTURED_SYMBOLS = true;

	protected final List<VDMMessage> errs = new Vector<VDMMessage>();
	protected final List<VDMMessage> warns = new Vector<VDMMessage>();
	
	public TCPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "TC";
	}

	@Override
	public void init()
	{
	}

	public void preCheck()
	{
		errs.clear();
		warns.clear();
	}
	
	public List<VDMMessage> getErrs()
	{
		return errs;
	}
	
	public List<VDMMessage> getWarns()
	{
		return warns;
	}
	
	abstract public <T> T getTC();
	
	abstract public <T> boolean checkLoadedFiles(T ast) throws Exception;

	abstract public JSONArray documentSymbols(File file);

	abstract public TCDefinition findDefinition(File file, int zline, int zcol);

	abstract public TCDefinitionList lookupDefinition(String startsWith);
}
