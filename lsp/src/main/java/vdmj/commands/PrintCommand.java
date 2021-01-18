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

import java.io.IOException;

import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.values.Value;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import dap.DAPServer;
import json.JSONObject;
import lsp.CancellableThread;
import vdmj.DAPDebugReader;
import workspace.DAPWorkspaceManager;
import workspace.Log;

public class PrintCommand extends Command
{
	public static final String USAGE = "Usage: print <expression>";
	public static final String[] HELP = { "print", "print <exp> - evaluate an expression" };
	
	private String expression;
	private static ExpressionExecutor executor;

	public PrintCommand(String line)
	{
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
	
	private class ExpressionExecutor extends CancellableThread
	{
		private final DAPRequest request;
		private final String expression;
		
		public ExpressionExecutor(DAPRequest request, String expression)
		{
			super("id");			// DAP request has no id?
			this.request = request;
			this.expression = expression;
		}
		
		@Override
		public void body()
		{
			DAPDebugReader dbg = null;
			DAPServer server = DAPServer.getInstance();
			DAPWorkspaceManager manager = DAPWorkspaceManager.getInstance();
			
			try
			{
				dbg = new DAPDebugReader();
				manager.setDebugReader(dbg);
				dbg.start();
				
				long before = System.currentTimeMillis();
				Value result = Interpreter.getInstance().execute(expression);
				long after = System.currentTimeMillis();
				
				String answer = "= " + result + "\nExecuted in " + (double)(after-before)/1000 + " secs.\n";
				
				server.writeMessage(new DAPResponse(request, true, null,
						new JSONObject("result", answer, "variablesReference", 0)));
			}
			catch (Exception e)
			{
				try
				{
					Log.error(e);
					server.writeMessage(new DAPResponse(request, false, e.getMessage(), null));
					server.writeMessage(stdout("Execution terminated."));
				}
				catch (IOException io)
				{
					Log.error(io);
				}
			}
			finally
			{
				if (dbg != null)
				{
					dbg.interrupt();	// Stop the debugger reader.
				}
				
				manager.setDebugReader(null);
				executor = null;
			}
		}
		
		@Override
		public void setCancelled()
		{
			super.setCancelled();
			Breakpoint.setExecCancelled();
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request, boolean wait)
	{
		if (executor != null)
		{
			return new DAPMessageList(request, false, "Still executing " + executor.expression, null);
		}
		else
		{
			if (!wait)
			{
				executor = new ExpressionExecutor(request, expression);
				executor.start();
				return null;
			}
			else
			{
				DAPDebugReader dbg = null;
				DAPWorkspaceManager manager = DAPWorkspaceManager.getInstance();

				try
				{
					dbg = new DAPDebugReader();
					manager.setDebugReader(dbg);
					dbg.start();
					
					long before = System.currentTimeMillis();
					Value result = Interpreter.getInstance().execute(expression);
					long after = System.currentTimeMillis();
					
					String answer = "= " + result + "\nExecuted in " + (double)(after-before)/1000 + " secs.\n";
					return new DAPMessageList(request, new JSONObject("result", answer, "variablesReference", 0));
				}
				catch (Exception e)
				{
					Log.error(e);
					DAPMessageList messages = new DAPMessageList(request, e);
					messages.add(stdout("Execution terminated."));
					return messages;
				}
				finally
				{
					if (dbg != null)
					{
						dbg.interrupt();	// Stop the debugger reader.
					}
					
					manager.setDebugReader(null);
				}
			}
		}
	}
	
	public static void setCancelled()
	{
		if (executor != null)
		{
			executor.setCancelled();
		}
		else
		{
			Log.error("Can't interrupt evaluation: no executor");
		}
	}
}
