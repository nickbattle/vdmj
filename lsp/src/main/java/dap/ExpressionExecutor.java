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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package dap;

import java.io.IOException;

import com.fujitsu.vdmj.config.Properties;

import json.JSONObject;

public class ExpressionExecutor extends AsyncExecutor
{
	private final String expression;
	private final boolean maximal;
	private String answer;

	public ExpressionExecutor(String id, DAPRequest request, String expression, boolean maximal)
	{
		super(id, request);
		this.expression = expression;
		this.maximal = maximal;
	}

	public ExpressionExecutor(String id, DAPRequest request, String expression)
	{
		this(id, request, expression, false);
	}

	@Override
	protected void head()
	{
		running = expression;
	}

	@Override
	protected void exec() throws Exception
	{
		boolean saved = Properties.parser_maximal_types;	// Used by qcrun
		
		try
		{
			Properties.parser_maximal_types = maximal;
			answer = manager.getInterpreter().execute(expression).toString();
		}
		finally
		{
			Properties.parser_maximal_types = saved;
		}
	}

	@Override
	protected void tail(double time) throws IOException
	{
		answer = "= " + answer + "\nExecuted in " + time + " secs.\n";
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
