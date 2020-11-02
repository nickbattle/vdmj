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

package workspace.plugins;

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
import lsp.LSPServer;
import rpc.RPCRequest;
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
	protected boolean completed = true;
	
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

	abstract public void preCheck();

	abstract public <T> boolean checkLoadedFiles(T inList) throws Exception;

	abstract public Map<String, TCNameList> getTraceNames();

	abstract public <T> T getCT();
	
	public int generate(TCNameToken tracename) throws Exception
	{
		Interpreter interpreter = DAPWorkspaceManager.getInstance().getInterpreter();
		interpreter.init();
		INNamedTraceDefinition tracedef = interpreter.findTraceDefinition(tracename);

		if (tracedef == null)
		{
			Log.error("Trace %s not found", tracename);
			throw new Exception("Trace " + tracename + " not found");
		}

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

	public void setFilter(TraceReductionType rType, float subset, long seed)
	{
		traceFilter = new TraceFilter(traceCount, subset, rType, seed);
	}

	public JSONArray execute(Object token, long startTest, long endTest) throws Exception
	{
		if (endTest > traceCount)
		{
			throw new Exception("Trace " + traceName + " only has " + traceCount + " tests");
		}
		
		if (endTest == 0)			// To the end of the tests, if specified as zero
		{
			endTest = traceCount;
		}
		
		if (startTest == 0)
		{
			startTest = 1;
		}
		else
		{
			// Suppress any reduction if a range specified
			traceFilter = new TraceFilter(traceCount, 1.0F, TraceReductionType.NONE, 0);
		}

		traceIterator.reset();
		testNumber = (int)startTest;
		completed = false;

		for (int i=1; i < startTest && traceIterator.hasMoreTests(); i++)
		{
			traceIterator.getNextTest();	// Skip first N tests
		}
		
		JSONArray batch = new JSONArray();
		
		if (traceIterator.hasMoreTests())
		{
			traceExecutor = new TraceExecutor(token, endTest);
			traceExecutor.start();
			traceExecutor.join();
			batch = traceExecutor.getResponses();
		}
		
		completed = true;
		return batch;
	}
	
	public synchronized boolean completed()
	{
		return completed;
	}
	
	public boolean generated()
	{
		return traceIterator != null;
	}

	private JSONArray runBatch(int batchSize, long endTest) throws Exception
	{
		Interpreter interpreter = DAPWorkspaceManager.getInstance().getInterpreter();
		JSONArray array = new JSONArray();

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
	    			Log.printf("Test " + testNumber + " = " + callString);
					Log.printf("Test " + testNumber + " FILTERED by test " + traceFilter.getFilteredBy(test));
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
	
	    			Log.printf("Test " + testNumber + " = " + callString);
	    			Log.printf("Result = " + result);
					array.add(new JSONObject(
							"id", testNumber,
							"verdict", getVerdict(result),
							"sequence", jsonResultPairs(callString, result)));
				}

				batchSize--;
			}
			
			testNumber++;
		}
		
		return array;
	}
	
	private int getVerdict(List<Object> result) throws Exception
	{
		for (int i = result.size()-1; i > 0; i--)
		{
			if (result.get(i) instanceof Verdict)
			{
				return jsonVerdict((Verdict)result.get(i));
			}
		}
		
		throw new Exception("No verdict returned?");
	}
	
	private int jsonVerdict(Verdict v) throws Exception
	{
		switch (v)
		{
			case PASSED:		return 1;
			case FAILED:		return 2;
			case INCONCLUSIVE:	return 3;
			case SKIPPED:		return 4;

			default: throw new Exception("Unknown verdict: " + v);
		}
	}
	
	private JSONArray jsonResultPairs(String callSeq, List<Object> results)
	{
		JSONArray array = new JSONArray();
		String[] calls = callSeq.split(";\\s+");
		
		for (int i=0; i<calls.length; i++)
		{
			array.add(new JSONObject("case", calls[i], "result",
				results == null ? null :
					i >= results.size() ? null :
						results.get(i).toString()));
		}
		
		return array;
	}
	
	private class TraceExecutor extends Thread
	{
		private Object progressToken;
		private long endTest;
		private JSONArray responses;
		private boolean cancelled;

		public TraceExecutor(Object progressToken, long endTest)
		{
			setName("CT TestExecutor");
			this.progressToken = progressToken;
			this.endTest = endTest;
			this.responses = new JSONArray();
			this.cancelled = false;
		}
		
		public JSONArray getResponses() throws InterruptedException
		{
			if (cancelled)
			{
				throw new InterruptedException("Trace execution cancelled");
			}
			
			return responses;
		}
		
		@Override
		public void run()
		{
			LSPServer server = LSPServer.getInstance();
			
			try
			{
				while (traceIterator.hasMoreTests())
				{
					JSONArray batch = runBatch(BATCH_SIZE, endTest);
					Thread.sleep(0);	// cancelled?
					
					if (progressToken == null)
					{
						responses.addAll(batch);
					}
					else
					{
						JSONObject params = new JSONObject("token", progressToken, "value", batch);
						
						if (server == null)
						{
							Log.printf("Sending intermediate results");
						}
						else
						{
							server.writeMessage(new RPCRequest("$/progress", params));
						}
					}
				}
			}
			catch (InterruptedException e)
			{
				Log.printf("CT Test Executor interrupted");
				cancelled = true;
			}
			catch (Exception e)
			{
				Log.error(e);
			}
			finally
			{
				completed = true;
			}
		}
	}
}
