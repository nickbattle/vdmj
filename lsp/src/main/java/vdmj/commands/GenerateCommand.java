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

import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.GenerateExecutor;
import lsp.LSPException;
import lsp.Utils;
import workspace.Diag;
import workspace.PluginRegistry;
import workspace.plugins.DAPPlugin;
import workspace.plugins.INPlugin;

public class GenerateCommand extends AnalysisCommand
{
	public static final String USAGE = "Usage: generate <trace name>";
	public static final String HELP = "generate <trace name> - debug expansion of trace name";
	
	public final String tracename;

	public GenerateCommand(String line)
	{
		super(line);
		
		if (argv.length == 2)
		{
			this.tracename = argv[1];
		}
		else
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		try
		{
			if (hasChanged())
			{
				return new DAPMessageList(request, false, "Specification has changed: try restart", null);
			}

			Interpreter interpreter = DAPPlugin.getInstance().getInterpreter();
			interpreter.init();
			TCNameToken qname = null;
			
			if (!tracename.contains("`"))
			{
				qname = Utils.stringToName(interpreter.getDefaultName() + "`" + tracename);
			}
			else
			{
				qname = Utils.stringToName(tracename);
			}
			
			INNamedTraceDefinition tracedef = interpreter.findTraceDefinition(qname);

			if (tracedef == null)
			{
				Diag.error("Trace %s not found", tracename);
				return new DAPMessageList(request, false, "Trace " + tracename + " not found", null);
			}

			GenerateExecutor executor = new GenerateExecutor("generate", request, tracedef);
			executor.start();
			return null;
		}
		catch (LSPException e)
		{
			return new DAPMessageList(request, e);
		}
	}

	@Override
	public boolean notWhenRunning()
	{
		return true;
	}

	private boolean hasChanged()
	{
		INPlugin in = PluginRegistry.getInstance().getPlugin("IN");
		return Interpreter.getInstance() != null && Interpreter.getInstance().getIN() != in.getIN();
	}
}
