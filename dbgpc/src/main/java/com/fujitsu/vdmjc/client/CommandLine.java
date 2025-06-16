/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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

package com.fujitsu.vdmjc.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fujitsu.vdmjc.common.Utils;


public class CommandLine
{
	protected final Dialect dialect;
	protected Release release = Release.DEFAULT;
	private String startLine = null;
	private final List<File> pathnames;

	protected static Queue<String> messages = new ConcurrentLinkedQueue<String>();

	public CommandLine(Dialect dialect, Release release, String startLine, List<File> pathnames)
	{
		this.dialect = dialect;
		this.release = release;
		this.startLine = startLine;
		this.pathnames = pathnames;
	}

	public static void message(String msg)
	{
		messages.add(msg);
	}

	protected void println(String line)
	{
		if (line.endsWith("\n"))
		{
			System.out.print(line);
		}
		else
		{
			System.out.println(line);
		}
	}

	protected void print(String line)
	{
		System.out.print(line);
		System.out.flush();
	}

	protected List<File> getFiles(String line) throws IOException
	{
		List<String> filenames = Arrays.asList(line.split("\\s+"));
		List<File> files = new Vector<File>();
		boolean OK = true;

		Iterator<String> it = filenames.iterator();
		it.next();	// to skip start "load" or "eval" etc.

		while (it.hasNext())
		{
			String name = it.next();
			File file = new File(name);

			if (file.exists())
			{
				if (file.isDirectory())
				{
 					for (File subfile: file.listFiles(dialect.getFilter()))
					{
						if (subfile.isFile())
						{
							files.add(subfile);
						}
					}
				}
    			else
    			{
   					files.add(file);
   				}
			}
			else
			{
				OK = false;

				for (File path: pathnames)
				{
					File pfile = new File(path, name);
					
					if (pfile.exists())
					{
						files.add(pfile);
						OK = true;
						break;
					}
				}

				if (!OK)
				{
					println("Cannot find file " + name);
				}
			}
		}

		if (!OK)
		{
			throw new IOException("Missing files");
		}

		return files;
	}

	protected String getPrompt() throws IOException
	{
   		return "> ";
	}

	protected String promptLine() throws IOException
	{
		if (startLine != null)
		{
			String line = startLine.trim();
			println("> " + startLine);
			startLine = null;
			return line;
		}

		StringBuilder lineTyped = new StringBuilder();
		InputStreamReader console = new InputStreamReader(System.in);
		boolean prompted = false;

		if (messages.isEmpty())
		{
			print(getPrompt());		// Otherwise nothing appears when quiet!
			prompted = true;
		}

		while (true)
		{
			if (!messages.isEmpty())
			{
				if (prompted)
				{
					print("\n");
				}

				while (!messages.isEmpty())
				{
					String msg = messages.poll();

					if (msg.length() > 0)
					{
						println(msg.toString());
					}
				}

				print(getPrompt());
				print(lineTyped.toString());
				prompted = true;
			}

			while (console.ready())
			{
				int c = console.read();

				if (c == '\r')
				{
					continue;
				}
				else if (c == '\n' || c == -1)
				{
					return lineTyped.toString().trim();
				}
				else
				{
					lineTyped.append((char)c);
				}
			}

			Utils.milliPause(10);
		}
	}

	public void run()
	{
		Thread.currentThread().setName("Command Line");
		boolean carryOn = true;

		while (carryOn)
		{
			try
            {
				String line = promptLine();

 	            if (line.equals(""))
	            {
	            	continue;
	            }
	            else if (line.equals("quit") || line.equals("q"))
				{
					carryOn = false;
				}
	            else if (line.startsWith("help"))
				{
					carryOn = processHelp(line);
				}
	            else if (line.equals("dbgp"))
				{
					carryOn = processDBGP();
				}
	            else if (line.equals("quiet"))
				{
					carryOn = processQuiet();
				}
	            else if (line.equals("ls") || line.equals("dir"))
				{
					carryOn = processLs();
				}
	            else if (line.startsWith("release"))
	            {
	            	carryOn = processRelease(line);
	            }
	            else if (line.startsWith("load"))
	            {
	            	carryOn = processLoad(line);
	            }
	            else if (line.startsWith("eval"))
	            {
	            	carryOn = processEval(line);
	            }
	            else
	            {
	            	println("Unknown command - try 'help'");
	            }
            }
            catch (Exception e)
            {
            	println("Failed: " + e.getMessage());
            }
		}

    	println("Bye");
	}

	protected boolean processLs()
	{
		try
		{
			File dir = new File(".");
			println("Directory: " + dir.getCanonicalPath());

			for (File f: dir.listFiles())
			{
				println(f.getName());
			}
		}
		catch (IOException e)
		{
			println("IO error: " + e.getMessage());
		}

		return true;
	}

	protected boolean processHelp(String line)
	{
		if (line.equals("help"))
		{
			println("Loading and starting:");
    		println("  load [<files>]");
    		println("  eval [<files>]");
    		println("  release " + Release.list());
    		println("  dbgp");
    		println("  quiet");
    		println("  help");
    		println("  ls | dir");
    		println("  q[uit]");
     		println("");
    		println("Use 'help <command>' for more help");
		}
		else
		{
			println("No more help yet...");
		}

		return true;
	}

	protected boolean processDBGP()
	{
		println("DBGp trace is now " +
			(ConnectionThread.setTrace() ? "ON" : "OFF"));
		return true;
	}

	protected boolean processRelease(String line)
	{
		String[] parts = line.split("\\s+");
		
		if (parts.length != 2)
		{
			println("Usage: release " + Release.list());
		}
		else
		{
			Release r = Release.lookup(parts[1].toLowerCase());
			
			if (r == null)
			{
				println("Usage: release " + Release.list());
			}
			else
			{
				release = r;
				println("Dialect is now " + dialect.name() + " " + release);
			}
		}
		
		return true;
	}

	protected boolean processQuiet()
	{
		println("Quiet setting is now " +
			(ConnectionThread.setQuiet() ? "ON" : "OFF"));
		return true;
	}

	protected boolean processLoad(String line)
	{
		try
		{
			new ProcessCommandLine(dialect, release, getFiles(line), "undefined").run();
		}
		catch (IOException e)
		{
			println("Problem loading files");
		}
		catch (Exception e)
		{
			println("Exception: " + e);
		}

		return true;
	}

	protected boolean processEval(String line)
	{
		try
		{
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			print("Evaluate: ");
			String expression = stdin.readLine().trim();

			while (expression.length() == 0)
			{
				println("Need an expression to evaluate");
				print("Evaluate: ");
				expression = stdin.readLine().trim();
			}

			new ProcessCommandLine(dialect, release, getFiles(line), expression).run();
		}
		catch (IOException e)
		{
			println("Problem loading files");
		}
		catch (Exception e)
		{
			println("Exception: " + e);
		}

		return true;
	}
}
