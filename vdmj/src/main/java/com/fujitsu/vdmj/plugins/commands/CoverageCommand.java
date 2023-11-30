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
import java.io.IOException;
import java.io.PrintWriter;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.SourceFile;

public class CoverageCommand extends AnalysisCommand
{
	private final static String CMD = "coverage clear|write <dir>|merge <dir>|<filenames>";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - handle line coverage";

	private Interpreter interpreter = null;

	public CoverageCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("coverage"))
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
					doCoverage(file);
				}

				return null;
			}

			if (argv.length == 2 && argv[1].equals("clear"))
			{
				LexLocation.clearLocations();
				return "Cleared all coverage information";
			}

			if (argv.length == 3 && argv[1].equals("write"))
			{
				writeCoverage(new File(argv[2]));
				return null;
			}

			if (argv.length == 3 && argv[1].equals("merge"))
			{
				mergeCoverage(new File(argv[2]));
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
						doCoverage(file);	// NB. don't use canonical files
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
			println(e.getMessage());
			return USAGE;
		}
	}

	private boolean doCoverage(File file)
	{
		try
		{
			SourceFile source = interpreter.getSourceFile(file);

			if (source == null)
			{
				println(file + ": file not found");
			}
			else
			{
				source.printCoverage(Console.out);
			}
		}
		catch (Exception e)
		{
			println("coverage: " + e.getMessage());
		}

		return true;
	}


	private void writeCoverage(File dir) throws IOException
    {
		if (dir.exists())
		{
			if (!dir.isDirectory())
			{
				println(dir + " is not a directory");
				return;
			}
		}
		else
		{
			println("Creating new directory: " + dir);
			dir.mkdirs();
		}
		
    	for (File f: interpreter.getSourceFiles())
    	{
    		SourceFile source = interpreter.getSourceFile(f);

    		File cov = new File(dir.getPath() + File.separator + f.getName() + ".cov");
    		PrintWriter pw = new PrintWriter(cov);
    		source.writeCoverage(pw);
    		pw.close();
    		println("Written coverage for " + f);
    	}
    }

	private void mergeCoverage(File dir) throws IOException
    {
		if (!dir.exists() || !dir.isDirectory())
		{
			println(dir + " is not an existing directory");
			return;
		}
		
    	for (File f: interpreter.getSourceFiles())
    	{
    		File cov = new File(dir.getPath() + File.separator + f.getName() + ".cov");
    		LexLocation.mergeHits(f, cov);
    		println("Merged coverage for " + f);
    	}
    }
}
