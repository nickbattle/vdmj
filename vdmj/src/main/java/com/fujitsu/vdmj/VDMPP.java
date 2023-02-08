/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.commands.ClassCommandReader;
import com.fujitsu.vdmj.commands.CommandReader;
import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.debug.ConsoleKeyWatcher;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.definitions.POClassList;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * The main class of the VDM++ and VICE parser/checker/interpreter.
 */
public class VDMPP extends VDMJ
{
	protected ASTClassList parsedClasses = new ASTClassList();
	private TCClassList checkedClasses = new TCClassList();
	private INClassList executableClasses = null;

	public VDMPP()
	{
		Settings.dialect = Dialect.VDM_PP;
	}

	/**
	 * @see com.fujitsu.vdmj.VDMJ#parse(java.util.List)
	 */

	@Override
	public ExitStatus parse(List<File> files)
	{
		parsedClasses.clear();
		LexLocation.resetLocations();
   		int perrs = 0;
   		int pwarn = 0;
   		long duration = 0;

   		for (File file: files)
   		{
   			ClassReader reader = null;

   			try
   			{
				long before = System.currentTimeMillis();
				LexTokenReader ltr = new LexTokenReader(file, Settings.dialect, Settings.filecharset);
    			reader = new ClassReader(ltr);
    			parsedClasses.addAll(reader.readClasses());
    	   		long after = System.currentTimeMillis();
    	   		duration += (after - before);
    		}
			catch (InternalException e)
			{
   				println(e.toString());
   				perrs++;
			}
			catch (Throwable e)
			{
   				println(e);
   				perrs++;
			}

			if (reader != null && reader.getErrorCount() > 0)
			{
    			perrs += reader.getErrorCount();
    			reader.printErrors(Console.out);
			}

			if (reader != null && reader.getWarningCount() > 0)
			{
				pwarn += reader.getWarningCount();
    			reader.printWarnings(Console.out);
			}
   		}

   		info("Parsed " + plural(parsedClasses.size(), "class", "es") + " in " +
   			(double)(duration)/1000 + " secs. ");
   		info(perrs == 0 ? "No syntax errors" :
   			"Found " + plural(perrs, "syntax error", "s"));
		infoln(pwarn == 0 ? "" : " and " +
			(warnings ? "" : "suppressed ") + plural(pwarn, "warning", "s"));

   		return perrs == 0 ? ExitStatus.EXIT_OK : ExitStatus.EXIT_ERRORS;
	}

	/**
	 * @see com.fujitsu.vdmj.VDMJ#typeCheck()
	 */
	@Override
	public ExitStatus typeCheck()
	{
		int terrs = 0;
		long before = System.currentTimeMillis();

   		try
   		{
   			checkedClasses = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsedClasses);
   			parsedClasses = new ASTClassList();		// AST not needed now
   			before = mapperStats(before, TCNode.MAPPINGS);
   			TypeChecker typeChecker = new ClassTypeChecker(checkedClasses);
   			typeChecker.typeCheck();
   		}
		catch (InternalException e)
		{
			println(e.toString());
		}
		catch (Throwable e)
		{
			println(e);
			terrs++;
		}

		long after = System.currentTimeMillis();
		terrs += TypeChecker.getErrorCount();

		if (terrs > 0)
		{
			TypeChecker.printErrors(Console.out);
		}

  		int twarn = TypeChecker.getWarningCount();

		if (twarn > 0 && warnings)
		{
			TypeChecker.printWarnings(Console.out);
		}

		info("Type checked " + plural(checkedClasses.size(), "class", "es") + " in " +
			(double)(after-before)/1000 + " secs. ");
  		info(terrs == 0 ? "No type errors" :
  			"Found " + plural(terrs, "type error", "s"));
		infoln(twarn == 0 ? "" : " and " +
			(warnings ? "" : "suppressed ") + plural(twarn, "warning", "s"));

  		if (pog && terrs == 0)
  		{
  			ProofObligationList list = null;
  			
      		try
      		{
      			long now = System.currentTimeMillis();
      			POClassList pogClasses = ClassMapper.getInstance(PONode.MAPPINGS).init().convert(checkedClasses);
      			mapperStats(now, PONode.MAPPINGS);
      			list = pogClasses.getProofObligations();
       		}
    		catch (InternalException e)
    		{
    			println(e.toString());
    		}
			catch (Throwable e)
			{
    			println(e);
    			terrs++;
			}

  			if (list == null || list.isEmpty())
  			{
  				println("No proof obligations generated");
  			}
  			else
  			{
  				println("Generated " + plural(list.size(), "proof obligation", "s") + ":\n");
  				print(list.toString());
  			}
  		}

   		return terrs == 0 ? ExitStatus.EXIT_OK : ExitStatus.EXIT_ERRORS;
	}

	/**
	 * @see com.fujitsu.vdmj.VDMJ#interpret(List, String)
	 */
	@Override
	protected ExitStatus interpret(List<File> filenames, String defaultName)
	{
		executableClasses = null;
		ClassInterpreter interpreter = null;

		if (logfile != null)
		{
    		try
    		{
    			RTLogger.setLogfileName(new File(logfile));
    			println("RT events now logged to " + logfile);
    		}
    		catch (FileNotFoundException e)
    		{
    			println("Cannot create RT event log: " + e.getMessage());
    			return ExitStatus.EXIT_ERRORS;
    		}
		}

		try
		{
   			long before = System.currentTimeMillis();
   			interpreter = getInterpreter();
   			if (Settings.verbose) before = System.currentTimeMillis();
  			ConsoleDebugReader dbg = null;
  			ConsoleKeyWatcher watcher = null;

   			try
   			{
   				dbg = new ConsoleDebugReader();
   				dbg.start();
   				watcher = new ConsoleKeyWatcher("init");
   				watcher.start();
   				
   				interpreter.init();
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

   			if (defaultName != null)
   			{
   				interpreter.setDefaultName(defaultName);
   			}

   			long after = System.currentTimeMillis();

   	   		infoln("Initialized " + plural(executableClasses.size(), "class", "es") + " in " +
   	   			(double)(after-before)/1000 + " secs. ");
		}
		catch (ContextException e)
		{
			println("Initialization: " + e);
			
			if (e.isStackOverflow())
			{
				e.ctxt.printStackFrames(Console.out);
			}
			else
			{
				e.ctxt.printStackTrace(Console.out, true);
			}

			return ExitStatus.EXIT_ERRORS;
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Initialization:");
			println(e);
			return ExitStatus.EXIT_ERRORS;
		}

		try
		{
			ExitStatus status;

			if (script != null)
			{
				println(interpreter.execute(script).toString());
				status = ExitStatus.EXIT_OK;
			}
			else
			{
				infoln("Interpreter started");
				CommandReader reader = new ClassCommandReader(interpreter, "> ");
				status = reader.run(filenames);
			}

			if (logfile != null)
			{
				RTLogger.dump(true);
				infoln("RT events dumped to " + logfile);
			}

			return status;
		}
		catch (ContextException e)
		{
			println("Execution: " + e);

			if (e.isStackOverflow())
			{
				e.ctxt.printStackFrames(Console.out);
			}
			else
			{
				e.ctxt.printStackTrace(Console.out, true);
			}
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Execution:");
			println(e);
		}

		return ExitStatus.EXIT_ERRORS;
	}

	@Override
	public ClassInterpreter getInterpreter() throws Exception
	{
		if (executableClasses == null)
		{
			long before = System.currentTimeMillis();
   			executableClasses = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checkedClasses);
   			mapperStats(before, INNode.MAPPINGS);
		}
		
		return new ClassInterpreter(executableClasses, checkedClasses);
	}
}
