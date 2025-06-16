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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package vdmj.commands;

import java.io.IOException;
import java.util.Map.Entry;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.modules.MultiModuleEnvironment;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.ExpressionExecutor;
import json.JSONObject;
import workspace.Diag;
import workspace.PluginRegistry;
import workspace.plugins.DAPPlugin;
import workspace.plugins.INPlugin;
import workspace.plugins.POPlugin;

public class PrintCommand extends AnalysisCommand implements InitRunnable, ScriptRunnable
{
	public static final String USAGE = "Usage: print <expression>";
	public static final String HELP = "print <exp> - evaluate an expression";
	
	public final String expression;

	public PrintCommand(String line)
	{
		super(line);
		String[] parts = line.split("\\s+", 2);
		
		if (parts.length == 2)
		{
			this.expression = parts[1];
		}
		else
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		ExpressionExecutor executor = new ExpressionExecutor("print", request, expression);
		executor.start();
		return null;
	}

	@Override
	public String initRun(DAPRequest request)
	{
		try
		{
			DAPPlugin manager = DAPPlugin.getInstance();
			JSONObject params = manager.getLaunchParams();
			Interpreter interpreter = manager.getInterpreter();
			Environment env = interpreter.getGlobalEnvironment();
			
			if (params != null)
			{
				if ("PO_LENS".equals(params.get("type")))
				{
					Diag.info("Processing PO code lens...");
					POPlugin po = PluginRegistry.getInstance().getPlugin("PO");
					
					if (po != null)
					{
						env = new MultiModuleEnvironment(po.getPO());
					}
				}
				
				JSONObject stateUpdates = params.get("state");
				
				if (stateUpdates != null && Settings.dialect == Dialect.VDM_SL)
				{
					Diag.info("Processing PO code lens state arguments");
					ModuleInterpreter m = ModuleInterpreter.getInstance();
					Context sctxt = m.getStateContext();
					
					if (sctxt != null)
					{
						INPlugin in = PluginRegistry.getInstance().getPlugin("IN");
						
						for (Entry<String, Object> entry: stateUpdates.entrySet())
						{
							Value newValue = in.evaluate(entry.getValue().toString());
							
							TCNameToken name = new TCNameToken(LexLocation.ANY, m.getDefaultName(), entry.getKey());
							UpdatableValue oldValue = (UpdatableValue) sctxt.get(name);

							if (oldValue != null)
							{
								Diag.info("Setting %s to %s", name, newValue);
								oldValue.set(LexLocation.ANY, newValue, m.getInitialContext());
							}
						}
					}
				}
			}
			
			return interpreter.execute(expression, env).toString();
		}
		catch (Exception e)
		{
			return "Cannot evaluate " + expression + " : " + e.getMessage();
		}
	}
	
	@Override
	public String getExpression()
	{
		return expression;
	}
	
	@Override
	public String format(String result)
	{
		return "= " + result;
	}

	@Override
	public boolean notWhenRunning()
	{
		return true;
	}
	
	@Override
	public boolean notWhenDirty()
	{
		return true;
	}

	@Override
	public String scriptRun(DAPRequest request) throws IOException
	{
		/**
		 * We are executing this on the main DAP thread from ScriptCommand. So we cannot
		 * stop at a breakpoint because the DAP thread is not listening for Client messages.
		 */
		return initRun(request);	// NB. No debug reader
	}
}
