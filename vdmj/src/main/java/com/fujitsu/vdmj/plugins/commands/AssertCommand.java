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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.plugins.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;
import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.messages.VDMErrorsException;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.Value;

public class AssertCommand extends AnalysisCommand
{
	private final static String CMD = "assert <file>";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - check assertions from a file";

	private boolean errors;

	public AssertCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("assert"))
		{
			throw new IllegalArgumentException(USAGE);
		}
		
		errors = false;
	}

	@Override
	public String run(String line)
	{
		if (argv.length != 2)
		{
			return USAGE;
		}
		
		File file = new File(argv[1]);
		
		if (!file.exists())
		{
			 return "File not found: " + file;
		}

		BufferedReader script = null;
		
		try
		{
			script = new BufferedReader(new InputStreamReader(new FileInputStream(file), Settings.filecharset));
			int assertErrors = 0;
			int assertPasses = 0;

			while (true)
			{
				String assertion = script.readLine();
				
				if (assertion == null)
				{
					break;
				}
				
				assertion = assertion.trim();
				
				if (assertion.isEmpty() || assertion.startsWith("--"))
				{
					continue;
				}

				try
				{
		   			Value result = Interpreter.getInstance().execute(assertion);

		   			if (!(result instanceof BooleanValue))
		   			{
	   					println("FAILED: " + assertion + " = " + result + " (Not boolean)");
	   					assertErrors++;
		   			}
		   			else if (!result.boolValue(null))
	   				{
	   					println("FAILED: " + assertion);
	   					assertErrors++;
	   				}
	   				else
	   				{
	   					assertPasses++;
	   				}
				}
				catch (LexException e)
				{
					println("FAILED: " + assertion);
					println("Lexical: " + e);
					assertErrors++;
				}
				catch (ParserException e)
				{
					println("FAILED: " + assertion);
					println("Syntax: " + e);
					assertErrors++;
				}
				catch (ContextException e)
				{
					println("FAILED: " + assertion);
					println("Runtime: " + e.getMessage());
					// e.ctxt.printStackTrace(Console.out, true);
					assertErrors++;
				}
				catch (VDMErrorsException e)
				{
					println("FAILED: " + assertion);
					println("Runtime: " + e.getMessage());
					// println(e);
					assertErrors++;
				}
				catch (Throwable e)
				{
					while (e instanceof InvocationTargetException)
					{
						e = e.getCause();
					}
					
					println("FAILED: " + assertion);
					println("Exception: " + e);
					println(e);
					assertErrors++;
				}
			}
			
			if (assertErrors == 0)
			{
				printf("PASSED all %d assertions from %s\n", assertPasses, file);
				errors = true;
			}
			else
			{
				printf("FAILED %d and passed %d assertions from %s\n", assertErrors, assertPasses, file);
			}
		}
		catch (IOException e)
		{
			errorln("Assert: " + e.getMessage());
		}
		finally
		{
			try
			{
				script.close();
			}
			catch (IOException e)
			{
				// ignore
			}
		}
		
		return null;
	}
	
	public boolean errors()
	{
		return errors;
	}
}
