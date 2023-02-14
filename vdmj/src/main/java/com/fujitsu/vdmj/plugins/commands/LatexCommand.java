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

public class LatexCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: latex|latexdoc [<files>]";
	private final Interpreter interpreter;

	public LatexCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("latex") && !argv[0].equals("latexdoc"))
		{
			throw new IllegalArgumentException(USAGE);
		}
		
		interpreter = Interpreter.getInstance();
	}

	@Override
	public void run()
	{
		try
		{
			boolean headers = (argv[0].equals("latexdoc"));
			
			if (argv.length == 1)
			{
				for (File file: interpreter.getSourceFiles())
				{
					doLatex(file, headers);
				}

				return;
			}

			for (int p = 1; p < argv.length; p++)
			{
				File farg = new File(argv[p]);
				boolean done = false;
				
				for (File file: interpreter.getSourceFiles())
    			{
					if (file.getCanonicalFile().equals(farg.getCanonicalFile()))
					{
						doLatex(file, headers);	// NB. don't use canonical files
						done = true;
						break;
					}
    			}

				if (!done)
    			{
    				println(farg + " is not loaded - try 'files'");
    			}
			}
		}
		catch (Exception e)
		{
			println("Usage: latex|latexdoc <filenames>");
		}
	}

	private void doLatex(File file, boolean headers)
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
				File tex = new File(source.filename.getPath() + ".tex");
				PrintWriter pw = new PrintWriter(tex, "UTF-8");
				source.printLatexCoverage(pw, headers);
				pw.close();
				println("Latex coverage written to " + tex);
			}
		}
		catch (Exception e)
		{
			println("latex: " + e.getMessage());
		}
	}
	
	public static void help()
	{
		println("latex|latexdoc [<files>] - generate LaTeX line coverage files");
	}
}
