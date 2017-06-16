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
 *
 ******************************************************************************/

package com.fujitsu.vdmj;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.commands.CommandReader;
import com.fujitsu.vdmj.commands.ModuleCommandReader;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.modules.POModuleList;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * The main class of the VDM-SL parser/checker/interpreter.
 */
public class VDMSL extends VDMJ
{
	private ASTModuleList parsedModules = new ASTModuleList();
	private TCModuleList checkedModules = new TCModuleList();
	private INModuleList executableModules = null;

	public VDMSL()
	{
		Settings.dialect = Dialect.VDM_SL;
	}

	/**
	 * @see com.fujitsu.vdmj.VDMJ#parse(java.util.List)
	 */

	@Override
	public ExitStatus parse(List<File> files)
	{
		parsedModules.clear();
		LexLocation.resetLocations();
   		int perrs = 0;
   		int pwarn = 0;
   		long duration = 0;

   		for (File file: files)
   		{
   			ModuleReader reader = null;

   			try
   			{
				long before = System.currentTimeMillis();
				LexTokenReader ltr = new LexTokenReader(file, Settings.dialect, filecharset);
    			reader = new ModuleReader(ltr);
    			parsedModules.addAll(reader.readModules());
    	   		long after = System.currentTimeMillis();
    	   		duration += (after - before);
    		}
			catch (InternalException e)
			{
   				println(e.toString());
			}
			catch (Throwable e)
			{
   				println(e.toString());
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
   		
   		int count = parsedModules.getModuleNames().size();

   		info("Parsed " + plural(count, "module", "s") + " in " +
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
		long before = System.currentTimeMillis();
		int terrs = 0;

   		try
   		{
   			checkedModules = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsedModules);
   			before = mapperStats(before, TCNode.MAPPINGS);
   			terrs += checkedModules.combineDefaults();

   			TypeChecker typeChecker = new ModuleTypeChecker(checkedModules);
   			typeChecker.typeCheck();
   		}
		catch (InternalException e)
		{
			println(e.toString());
		}
		catch (Throwable e)
		{
			println(e.toString());
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

		info("Type checked " + plural(checkedModules.size(), "module", "s") +
			" in " + (double)(after-before)/1000 + " secs. ");
  		info(terrs == 0 ? "No type errors" :
  			"Found " + plural(terrs, "type error", "s"));
  		infoln(twarn == 0 ? "" : " and " +
  			(warnings ? "" : "suppressed ") + plural(twarn, "warning", "s"));

  		if (pog && terrs == 0)
  		{
  			ProofObligationList list = null;
  			
      		try
      		{
      			POModuleList pogModules = ClassMapper.getInstance(PONode.MAPPINGS).init().convert(checkedModules);
      			list = pogModules.getProofObligations();
       		}
    		catch (InternalException e)
    		{
    			println(e.toString());
    		}
			catch (Throwable e)
			{
    			println(e.toString());
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
		ModuleInterpreter interpreter = null;
		executableModules = null;

		try
		{
   			long before = System.currentTimeMillis();
   			interpreter = getInterpreter();

   			interpreter.init();

   			if (defaultName != null)
   			{
   				interpreter.setDefaultName(defaultName);
   			}

   			long after = System.currentTimeMillis();

   	   		infoln("Initialized " + plural(parsedModules.size(), "module", "s") + " in " +
   	   			(double)(after-before)/1000 + " secs. ");
		}
		catch (ContextException e)
		{
			println("Initialization: " + e);
			e.ctxt.printStackTrace(Console.out, true);
			return ExitStatus.EXIT_ERRORS;
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Initialization: " + e.getMessage());
			return ExitStatus.EXIT_ERRORS;
		}

		try
		{
			if (script != null)
			{
				println(interpreter.execute(script).toString());
				return ExitStatus.EXIT_OK;
			}
			else
			{
				infoln("Interpreter started");
				CommandReader reader = new ModuleCommandReader(interpreter, "> ");
				return reader.run(filenames);
			}
		}
		catch (ContextException e)
		{
			println("Execution: " + e);
			e.ctxt.printStackTrace(Console.out, true);
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Execution: " + e);
		}

		return ExitStatus.EXIT_ERRORS;
	}

	@Override
	public ModuleInterpreter getInterpreter() throws Exception
	{
		if (executableModules == null)
		{
			long before = System.currentTimeMillis();
   			executableModules = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checkedModules);
   			mapperStats(before, INNode.MAPPINGS);
		}
		
		return new ModuleInterpreter(executableModules, checkedModules);
	}
}
