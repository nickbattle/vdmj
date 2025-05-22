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
import java.io.PrintWriter;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.BacktrackInputReader;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.SourceFile;

public class SaveCommand extends AnalysisCommand
{
	private final static String CMD = "save [<filenames>]";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - generate external source extract files";
	
	private final Interpreter interpreter;

	public SaveCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("save"))
		{
			throw new IllegalArgumentException(USAGE);
		}

		interpreter = Interpreter.getInstance();
	}

	@Override
	public String run(String line)
	{
		try
		{
			if (argv.length == 1)
			{
				for (File file: interpreter.getSourceFiles())
				{
					doSave(file);
				}

				return null;
			}

			for (int p = 1; p < argv.length; p++)
			{
				File farg = new File(argv[p]);
				boolean done = false;
				
				for (File file: interpreter.getSourceFiles())
    			{
					if (file.getCanonicalFile().equals(farg.getCanonicalFile()))
					{
						doSave(file);	// NB. don't use canonical files
						done = true;
						break;
					}
    			}

				if (!done)
    			{
    				println(farg + " is not loaded - try 'files'");
    			}
			}
			
			return null;
		}
		catch (Exception e)
		{
			return USAGE;
		}
	}

	protected void doSave(File file)
	{
		try
		{
			if (BacktrackInputReader.isExternalFormat(file))
			{
    			SourceFile source = interpreter.getSourceFile(file);

    			if (source == null)
    			{
    				println(file + ": file not found");
    			}
    			else
    			{
    				File vdm = new File(source.filename.getPath() + "." +
    					Settings.dialect.getArgstring().substring(1));
    				PrintWriter spw = new PrintWriter(vdm, "UTF-8");
    				source.printSource(spw);
    				spw.close();
    				println("Extracted source written to " + vdm);
    			}
			}
			else
			{
				println("Not an external format file: " + file);
			}
		}
		catch (Exception e)
		{
			println("save: " + e.getMessage());
		}
	}
}
