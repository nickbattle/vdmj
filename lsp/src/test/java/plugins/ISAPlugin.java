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

package plugins;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;

import json.JSONObject;
import workspace.Diag;
import workspace.plugins.AnalysisPlugin;

public abstract class ISAPlugin extends AnalysisPlugin
{
	public static ISAPlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			case VDM_SL:
				return new ISAPluginSL();
				
			default:
				Diag.error("Unknown dialect " + dialect);
				throw new RuntimeException("Unsupported dialect: " + Settings.dialect);
		}
	}

	protected ISAPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "ISA";
	}

	@Override
	public void init()
	{
		// Ignore
	}
	
	@Override
	public JSONObject getExperimentalOptions()
	{
		return new JSONObject("isabelle", "server-option");
	}
}