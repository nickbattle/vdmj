/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package dap;

import java.io.IOException;

import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.scheduler.MainThread;
import com.fujitsu.vdmj.values.CPUValue;

import json.JSONObject;
import lsp.LSPException;
import vdmj.INGenerateExpression;

public class GenerateExecutor extends AsyncExecutor
{
	private final INNamedTraceDefinition tracedef;
	private String answer;

	public GenerateExecutor(String id, DAPRequest request, INNamedTraceDefinition def) throws LSPException
	{
		super(id, request);
		this.tracedef = def;
	}

	@Override
	protected void head()
	{
		running = "generate " + tracedef.name;
	}

	@Override
	protected void exec() throws Exception
	{
		Interpreter i = Interpreter.getInstance();
		Context mainContext = i.getTraceContext(tracedef.classDefinition);
		mainContext.putAll(i.getInitialContext());
		mainContext.setThreadState(CPUValue.vCPU);
		i.clearBreakpointHits();
		
		INExpression gexp = new INGenerateExpression(tracedef);
		MainThread main = new MainThread(gexp, mainContext);
		main.start();
		i.getScheduler().start(main);

		answer = "Generated " + main.getResult() + " tests";	// Can throw ContextException
	}

	@Override
	protected void tail(double time) throws IOException
	{
		answer = answer + "\nExecuted in " + time + " secs.\n";
		server.writeMessage(new DAPResponse(request, true, null,
				new JSONObject("result", answer, "variablesReference", 0)));
	}

	@Override
	protected void error(Throwable e) throws IOException
	{
		server.writeMessage(new DAPResponse(request, false, e.getMessage(), null));
		server.stdout("Execution terminated.");
	}

	@Override
	protected void clean()
	{
		running = null;
	}
}
