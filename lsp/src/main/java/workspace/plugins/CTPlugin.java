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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.traces.CallSequence;
import com.fujitsu.vdmj.traces.TraceFilter;
import com.fujitsu.vdmj.traces.TraceIterator;
import com.fujitsu.vdmj.traces.TraceReductionType;
import com.fujitsu.vdmj.traces.Verdict;

import json.JSONArray;
import json.JSONObject;
import lsp.CancellableThread;
import lsp.LSPException;
import lsp.LSPServer;
import rpc.RPCErrors;
import rpc.RPCRequest;
import rpc.RPCResponse;
import workspace.DAPWorkspaceManager;
import workspace.Log;

abstract public class CTPlugin extends AnalysisPlugin
{
	protected TraceIterator traceIterator = null;
	protected INClassDefinition traceClassDef = null;
	protected Context traceContext = null;
	protected int traceCount = 0;
	protected TCNameToken traceName = null;
	protected TraceFilter traceFilter = null;
	
	protected int testNumber = 0;
	protected TraceExecutor traceExecutor = null;
	protected boolean traceRunning = false;
	
	private static final int BATCH_SIZE = 10;
	
	public CTPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "CT";
	}

	@Override
	public void init()
	{
	}
	
	@Override
	public JSONObject getExperimentalOptions()
	{
		return new JSONObject("combinatorialTestProvider", new JSONObject("workDoneProgress", true));
	}

	public void preCheck()
	{
		if (traceExecutor != null && traceRunning)
		{
			try
			{
				traceExecutor.setCancelled();
				traceExecutor.join();
			}
			catch (InterruptedException e)
			{
				// Ignore
			}
		}
	}

	abstract public <T> boolean checkLoadedFiles(T inList) throws Exception;

	abstract public Map<String, TCNameList> getTraceNames();

	abstract public <T> T getCT();
	
	public int generate(TCNameToken tracename) throws LSPException
	{
		Interpreter interpreter = DAPWorkspaceManager.getInstance().getInterpreter();
		interpreter.init();
		INNamedTraceDefinition tracedef = interpreter.findTraceDefinition(tracename);

		if (tracedef == null)
		{
			Log.error("Trace %s not found", tracename);
			throw new LSPException(RPCErrors.ContentModified, "Trace " + tracename + " not found");
		}

		try
		{
			long before = System.currentTimeMillis();
			traceName = tracename;
			traceClassDef = tracedef.classDefinition;
			traceContext = interpreter.getTraceContext(traceClassDef);
			traceIterator = tracedef.getIterator(traceContext);
			traceCount = traceIterator.count();
			long after = System.currentTimeMillis();

			Log.printf("Generated %d traces in %.3f secs.", traceCount, (double)(after-before)/1000);
			return traceCount;
		}
		catch (Exception e)
		{
			throw new LSPException(RPCErrors.InternalError, e.getMessage());
		}
	}

	public JSONArray execute(RPCRequest request, TCNameToken tracename,
			Object progressToken, Object workDoneToken,
			TraceReductionType rType, float subset, long seed,
			Long startTest, Long endTest) throws LSPException
	{
		if (!tracename.equals(traceName))
		{
			Log.printf("Pre-generating new tracename %s", tracename);
			generate(tracename);
		}
		
		if (startTest == null && endTest != null && endTest != traceCount)
		{
			throw new LSPException(RPCErrors.ContentModified,
					"Trace " + traceName + " has " + traceCount + " tests");
		}
		
		if (endTest != null && endTest > traceCount)
		{
			throw new LSPException(RPCErrors.ContentModified,
					"Trace " + traceName + " only has " + traceCount + " tests");
		}
		
		if (endTest == null)		// To the end of the tests, if not specified
		{
			endTest = (long) traceCount;
		}
		
		if (startTest == null || startTest == 1)
		{
			startTest = 1L;
			traceFilter = new TraceFilter(traceCount, subset, rType, seed);
		}
		else
		{
			// Suppress any reduction if a range specified
			traceFilter = new TraceFilter(traceCount, 1.0F, TraceReductionType.NONE, 0);
		}

		traceIterator.reset();
		testNumber = startTest.intValue();
		traceRunning = true;

		for (int i=1; i < startTest && traceIterator.hasMoreTests(); i++)
		{
			traceIterator.getNextTest();	// Skip first N-1 tests
		}
		
		if (traceIterator.hasMoreTests())
		{
			traceExecutor = new TraceExecutor(request, progressToken, workDoneToken, startTest, endTest);
			traceExecutor.start();
			return null;
		}
		else
		{
			traceRunning = false;
			return new JSONArray();		// Empty result
		}
	}
	
	public JSONObject runtrace(TCNameToken tracename, long testNumber) throws LSPException
	{
		if (!tracename.equals(traceName))
		{
			Log.printf("Pre-generating new tracename %s", tracename);
			generate(tracename);
		}
		
		traceIterator.reset();

		for (int i=1; i < testNumber && traceIterator.hasMoreTests(); i++)
		{
			traceIterator.getNextTest();	// Skip first N-1 tests
		}

		CallSequence test = traceIterator.getNextTest();
		String callString = test.getCallString(traceContext);
		Interpreter interpreter = DAPWorkspaceManager.getInstance().getInterpreter();

		interpreter.init();
		List<Object> result = interpreter.runOneTrace(traceClassDef, test, true);

		return new JSONObject(
				"id", testNumber,
				"verdict", getVerdict(result),
				"sequence", jsonResultPairs(callString, result));
	}
	
	public synchronized boolean isRunning()
	{
		return traceRunning;
	}

	private class TraceExecutor extends CancellableThread
	{
		private final RPCRequest request;
		private final Object progressToken;
		private final Object workDoneToken;
		private final long startTest;
		private final long endTest;
		private final JSONArray responses;

		public TraceExecutor(RPCRequest request, Object progressToken, Object workDoneToken,
			long startTest, long endTest)
		{
			super(request.get("id"));
			this.request = request;
			this.progressToken = progressToken;
			this.workDoneToken = workDoneToken;
			this.startTest = startTest;
			this.endTest = endTest;
			this.responses = new JSONArray();

			setName("TraceExecutor");
		}
		
		@Override
		public void body()
		{
			LSPServer server = LSPServer.getInstance();
			long percentDone = -1;	// ie. not started
			
			try
			{
				while (traceIterator.hasMoreTests() && testNumber <= endTest)
				{
					JSONArray batch = runBatch(BATCH_SIZE, endTest);
					
					if (workDoneToken != null)
					{
						long done = (100 * (testNumber - startTest - 1))/(endTest - startTest + 1);
						
						if (done != percentDone)	// Only if changed %age
						{
							JSONObject value = null;
							
							if (percentDone < 0)
							{
								value = new JSONObject(
									"kind",			"begin",
									"title",		"Executing Combinatorial Tests",
									"message",		"Processing " + traceName,
									"percentage",	done);
							}
							else
							{
								value = new JSONObject(
									"kind",			"report",
									"message",		"Processing " + traceName,
									"percentage",	done);
							}
							
							JSONObject params = new JSONObject("token", workDoneToken, "value", value);
							Log.printf("Sending work done = %d%%", done);
							send(server, RPCRequest.notification("$/progress", params));
							percentDone = done;
						}
					}
					
					if (progressToken == null)
					{
						responses.addAll(batch);
					}
					else
					{
						JSONObject params = new JSONObject("token", progressToken, "value", batch);
						Log.printf("Sending intermediate results");
						send(server, RPCRequest.notification("$/progress", params));
					}

					if (cancelled)
					{
						Log.printf("%s cancelled", getName());
						break;
					}
				}
				
				if (progressToken == null)
				{
					if (cancelled)
					{
						Log.printf("Sending cancelled results");
						send(server, RPCResponse.error(request, RPCErrors.RequestCancelled, "Trace cancelled", responses));
					}
					else
					{
						Log.printf("Sending complete results");
						send(server, RPCResponse.result(request, responses));
					}
				}
				else
				{
					if (cancelled)
					{
						Log.printf("Sending cancelled null result");
						send(server, RPCResponse.error(request, RPCErrors.RequestCancelled, "Trace cancelled", null));
					}
					else
					{
						Log.printf("Sending final null result");
						send(server, RPCResponse.result(request));
					}
				}
			}
			catch (Throwable e)
			{
				Log.error(e);
			}
			finally
			{
				traceRunning = false;
				traceExecutor = null;
			}
		}
		
		private void send(LSPServer server, JSONObject response) throws IOException
		{
			if (server == null)		// Unit testing
			{
				Log.printf("%s", response.toString());
			}
			else
			{
				server.writeMessage(response);
			}
		}

		private JSONArray runBatch(int batchSize, long endTest) throws Exception
		{
			Interpreter interpreter = DAPWorkspaceManager.getInstance().getInterpreter();
			JSONArray array = new JSONArray();
			
			Log.printf("Starting batch at test number %d...", testNumber);
		
			while (traceIterator.hasMoreTests() && batchSize > 0 && testNumber <= endTest)
			{
				CallSequence test = traceIterator.getNextTest();
				
				if (traceFilter.isRemoved(test, testNumber))
				{
					// skip
				}
				else
				{
					String callString = test.getCallString(traceContext);
					
					if (traceFilter.getFilteredBy(test) > 0)
					{
						array.add(new JSONObject(
								"id", testNumber,
								"verdict", jsonVerdict(Verdict.SKIPPED),
								"sequence", jsonResultPairs(callString, null)));
					}
					else
					{
		    			interpreter.init();	// Initialize completely between every run...
		    			List<Object> result = interpreter.runOneTrace(traceClassDef, test, false);
		    			traceFilter.update(result, test, testNumber);
		
						array.add(new JSONObject(
								"id", testNumber,
								"verdict", getVerdict(result),
								"sequence", jsonResultPairs(callString, result)));
					}
		
					batchSize--;
				}
				
				testNumber++;
			}
			
			Log.printf("Completed batch at test number %d", testNumber);
			return array;
		}
	}

	private long getVerdict(List<Object> result) throws LSPException
	{
		for (int i = result.size()-1; i > 0; i--)
		{
			if (result.get(i) instanceof Verdict)
			{
				return jsonVerdict((Verdict)result.get(i));
			}
		}
		
		throw new LSPException(RPCErrors.InternalError, "No verdict returned?");
	}

	private long jsonVerdict(Verdict v) throws LSPException
	{
		switch (v)
		{
			case PASSED:		return 1;
			case FAILED:		return 2;
			case INCONCLUSIVE:	return 3;
			case SKIPPED:		return 4;
	
			default: throw new LSPException(RPCErrors.InternalError, "Unknown verdict: " + v);
		}
	}

	private JSONArray jsonResultPairs(String callSeq, List<Object> results)
	{
		JSONArray array = new JSONArray();
		String[] calls = callSeq.split(";\\s+");
		
		for (int i=0; i<calls.length; i++)
		{
			String result = null;
			
			if (results != null && i < results.size())
			{
				result = results.get(i).toString();
			}
			
			array.add(new JSONObject("case", calls[i], "result", result));
		}
		
		return array;
	}
}
