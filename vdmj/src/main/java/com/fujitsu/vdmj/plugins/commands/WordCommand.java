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

import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.SourceFile;

public class WordCommand extends AnalysisCommand
{
	private final static String CMD = "word [<files>]";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - generate Word HTML line coverage files";

	private final Interpreter interpreter;

	public WordCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("word"))
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
					doWord(file);
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
						doWord(file);	// NB. don't use canonical files
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
			return "Usage: word [<filenames>]";
		}
	}

	private void doWord(File file)
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
				File html = new File(source.filename.getPath() + ".doc");
				PrintWriter pw = new PrintWriter(html, "UTF-8");
				source.printWordCoverage(pw);
				pw.close();
				println("Word HTML coverage written to " + html);
			}
		}
		catch (Exception e)
		{
			println("word: " + e.getMessage());
		}
	}
}
