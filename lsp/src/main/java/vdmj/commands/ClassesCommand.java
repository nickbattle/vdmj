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

package vdmj.commands;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.runtime.ClassInterpreter;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import workspace.Diag;

public class ClassesCommand extends AnalysisCommand
{
	public static final String USAGE = "Usage: classes";
	public static final String HELP = "classes - list the classes in the specification";
	
	public ClassesCommand(String line)
	{
		if (!line.trim().equals("classes"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		try
		{
			if (Settings.dialect == Dialect.VDM_SL)
			{
				return new DAPMessageList(request,
						false, "Command not available for VDM-SL", null);			
			}
			
			ClassInterpreter  m = ClassInterpreter.getInstance();
			String defname = m.getDefaultName();
			StringBuilder sb = new StringBuilder();
			
			for (INClassDefinition module: m.getClasses())
			{
				sb.append(module.name.toString());
				if (module.name.toString().equals(defname)) sb.append(" (default)");
				sb.append("\n");
			}

			return new DAPMessageList(request, new JSONObject("result", sb.toString()));
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new DAPMessageList(request, e);
		}
	}

	@Override
	public boolean notWhenRunning()
	{
		return false;
	}
}
