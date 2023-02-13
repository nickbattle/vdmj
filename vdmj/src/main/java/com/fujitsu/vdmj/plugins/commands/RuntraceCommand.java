/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package com.fujitsu.vdmj.plugins.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.debug.ConsoleKeyWatcher;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.in.modules.INModule;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.ConsolePrintWriter;
import com.fujitsu.vdmj.messages.ConsoleWriter;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.messages.VDMErrorsException;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.DebuggerException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.traces.TraceReductionType;
import com.fujitsu.vdmj.util.Utils;

public class RuntraceCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: runtrace | debugtrace | runalltraces | savetrace | seedtrace | filter";
	private Interpreter interpreter;

	private static TraceReductionType reductionType = TraceReductionType.NONE;
	private static float reduction = 1.0F;
	private static File traceoutput = null;
	private static long traceseed = 0;

	public RuntraceCommand(String[] argv)
	{
		super(argv);
		
		if (!argv[0].equals("runtrace") && !argv[0].equals("rt") &&
			!argv[0].equals("debugtrace") && !argv[0].equals("dt") &&
			!argv[0].equals("runalltraces") &&
			!argv[0].equals("savetrace") &&
			!argv[0].equals("seedtrace") &&
			!argv[0].equals("filter"))
		{
			throw new IllegalArgumentException(USAGE);
		}
		
		interpreter = Interpreter.getInstance();
	}

	@Override
	public void run()
	{
		switch (argv[0])
		{
			case "runtrace":
			case "rt":
				doRuntrace(false);
				break;
				
			case "debugtrace":
			case "dt":
				doRuntrace(true);
				break;
				
			case "runalltraces":
				doAllTraces();
				break;
				
			case "savetrace":
				doSavetrace();
				break;
				
			case "seedtrace":
				doSeedtrace();
				break;
				
			case "filter":
				doFilter();
				break;
		}
	}

	private void doRuntrace(boolean debug)
	{
		doRuntrace(argv, debug);
	}

	private void doRuntrace(String line, boolean debug)
	{
		doRuntrace(Utils.toArgv(line), debug);
	}

	private void doRuntrace(String[] argv, boolean debug)
	{
		int startTest = 0;
		int endTest = 0;

		if (argv.length == 1)
		{
			println("Usage: " + argv[0] + " <name> [start test [end test]]");
			return;
		}
		else if (argv.length == 3)
		{
			try
			{
				startTest = Integer.parseInt(argv[2]);
				endTest = startTest;
			}
			catch (NumberFormatException e)
			{
				println("Usage: " + argv[0] + " <name> [start number]");
				return;
			}
		}
		else if (argv.length == 4)
		{
			try
			{
				startTest = Integer.parseInt(argv[2]);
				endTest = argv[3].equalsIgnoreCase("end") ? 0 : Integer.parseInt(argv[3]);
			}
			catch (NumberFormatException e)
			{
				println("Usage: " + argv[0] + " <name> [start number [end number or \"end\"]]");
				return;
			}
		}

		try
		{
			ConsoleWriter out = null;
			
			if (!debug && traceoutput != null)
			{
				try
				{
					out = new ConsolePrintWriter(traceoutput);
					Interpreter.setTraceOutput(out);
					println("Trace output sent to " + traceoutput);
				}
				catch (Exception e)
				{
					println("Cannot create output file " + traceoutput);
					return;
				}
			}
			
			ConsoleDebugReader dbg = null;
			ConsoleKeyWatcher watcher = null;
			
			try
			{
				dbg = new ConsoleDebugReader();
				dbg.start();
				watcher = new ConsoleKeyWatcher("runtrace");
				watcher.start();

				boolean passed = interpreter.runtrace(argv[1], startTest, endTest, debug, reduction, reductionType, traceseed);
    			
    			if (!debug && traceoutput != null)
    			{
    				out.close();
    				Interpreter.setTraceOutput(null);
    			}
    
    			if (passed)
    			{
    				println("All tests passed");
    			}
    			else
    			{
    				println("Some tests failed or indeterminate");
    			}
			}
			finally
			{
				if (dbg != null)
				{
					dbg.interrupt();
				}
				
				if (watcher != null)
				{
					watcher.interrupt();
				}
			}

			if (RTLogger.getLogSize() > 0)
			{
				println("Dumped RT events");
				RTLogger.dump(false);
			}
		}
		catch (ParserException e)
		{
			println("Syntax: " + e.getMessage());
		}
		catch (DebuggerException e)
		{
			println("Debug: " + e.getMessage());
		}
		catch (RuntimeException e)
		{
			println("Runtime: " + e);
		}
		catch (VDMErrorsException e)
		{
			println(e.toString());
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Error: " + e.getMessage());
		}
	}
	
	private void doAllTraces()
	{
		String pattern = ".*";
		
		if (argv.length == 2)
		{
			pattern = argv[1];
		}
		else
		{
			println("Usage: runalltraces <pattern>");
		}
		
		if (Settings.dialect == Dialect.VDM_SL)
		{
			ModuleInterpreter minterpreter = (ModuleInterpreter)interpreter;
			
			for (INModule m: minterpreter.getModules())
			{
				if (m.name.getName().matches(pattern))
				{
	    			for (INDefinition d: m.defs)
	    			{
	    				if (d instanceof INNamedTraceDefinition)
	    				{
	    					String cmd = "runtrace " + d.name.getExplicit(true);
	    					println("-------------------------------------");
	    					println(cmd);
	    					doRuntrace(cmd, false);
	    				}
	    			}
				}
			}
		}
		else
		{
			ClassInterpreter cinterpreter = (ClassInterpreter)interpreter;
			
			for (INClassDefinition cdef: cinterpreter.getClasses())
			{
				if (cdef.name.getName().matches(pattern))
				{
	    			for (INDefinition d: cdef.definitions)
	    			{
	    				if (d instanceof INNamedTraceDefinition)
	    				{
	    					String cmd = "runtrace " + d.name.getExplicit(true);
	    					println("-------------------------------------");
	    					println(cmd);
	    					doRuntrace(false);
	    				}
	    			}
				}
			}
		}
	}

	private void doSavetrace()
	{
		if (argv.length == 1)
		{
			if (traceoutput == null)
			{
				println("runtrace output is to console");
			}
			else
			{
				println("runtrace output is redirected to " + traceoutput);
			}
		}
		else if (argv.length != 2)
		{
			println("savetrace <file> | OFF");
		}
		else
		{
			if (argv[1].equalsIgnoreCase("off"))
			{
				traceoutput = null;
				println("runtrace output is to console");
			}
			else
			{
				traceoutput = new File(argv[1]);
				println("runtrace output redirected to " + traceoutput);
			}
		}
	}

	protected void doSeedtrace()
	{
		if (argv.length != 2)
		{
			println("seedtrace <number>");
		}
		else
		{
			try
			{
				traceseed = Long.parseLong(argv[1]);
				println("Trace filter currently " + reduction*100 + "% " + reductionType + " (seed " + traceseed + ")");
			}
			catch (NumberFormatException e)
			{
				println("seedtrace <number>");
			}
		}
	}
	
	private void doFilter()
	{
		if (argv.length != 2)
		{
			println("Usage: filter %age | RANDOM | SHAPES_NOVARS | SHAPES_VARNAMES | SHAPES_VARVALUES | NONE");
		}
		else
		{
			try
			{
				if (argv[1].endsWith("%"))
				{
					argv[1] = argv[1].substring(0, argv[1].lastIndexOf('%'));
				}
				
				float val = Float.parseFloat(argv[1]) / 100.0F;

				if (val > 1 || val <= 0)
				{
					throw new NumberFormatException("Should be 1-100");
				}
				
				reduction = val;
				
				if (reductionType == TraceReductionType.NONE)
				{
					reductionType = TraceReductionType.RANDOM;
				}
			}
			catch (NumberFormatException e)
			{
				try
				{
					reductionType = TraceReductionType.valueOf(argv[1].toUpperCase());
					
					if (reductionType == TraceReductionType.NONE)
					{
						reduction = (float)1.0;
					}
				}
				catch (Exception e1)
				{
					println("Usage: filter %age | RANDOM | SHAPES_NOVARS | SHAPES_VARNAMES | SHAPES_VARVALUES | NONE");
				}
			}
		}

		println("Trace filter currently " + reduction*100 + "% " + reductionType + " (seed " + traceseed + ")");
	}

	public static void help()
	{
		println("runtrace <name> [start test [end test]] - run CT trace");
		println("debugtrace <name> [start test [end test]] - debug CT trace");
		println("savetrace [<file> | off] - save CT trace output");
		println("seedtrace <number> - seed CT trace random generator");
		println("runalltraces [<name>] - run all CT traces in class/module name");
		println("filter %age | <reduction type> - reduce CT trace(s)");
	}
}
