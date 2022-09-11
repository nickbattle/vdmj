/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj.lex;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.messages.VDMError;

/**
 * A class to process #ifdef #else #endif directives.
 */
public class IfdefProcessor
{
	private final File sourceFile;
	private final List<VDMError> errs;

	private BufferedReader input = null;
	private StringBuilder output = null;
	private String line = null;
	private int lineNo;
	
	public IfdefProcessor(File file)
	{
		this.sourceFile = file;
		this.errs = new Vector<VDMError>();
	}
	
	public char[] getText(char[] source) throws IOException
	{
		String text = new String(source);
		input = new BufferedReader(new StringReader(text));
		output = new StringBuilder();
		
		line = input.readLine();
		lineNo = 1;
		readBody(true);
		
		if (line != null)	// Should be null at EOF
		{
			String trimmed = line.trim();
			String[] parts = trimmed.split("\\s+");
			
			if (parts[0].equals("#else") || parts[0].equals("#endif") || parts[0].equals("#elseif"))
			{
				error(1500, "Unexpected " + parts[0], parts[0]);
			}
			else
			{
				error(1501, "Unexpected end of preprocessor stream", parts[0]);
			}
		}

		input.close();
		return output.toString().toCharArray();
	}
	
	private void error(int errno, String message, String token)
	{
		LexLocation loc = new LexLocation(sourceFile, null, lineNo, 1, lineNo, token.length()+1);
		errs.add(new VDMError(errno, message, loc));
	}
	
	private void writeLine(boolean include) throws IOException
	{
		if (include) output.append(line);
		output.append("\n");
		
		line = input.readLine();
		lineNo++;
	}
	
	/**
	 * A body is a section that may contain plain text, or #ifdef directives.
	 * The include argument indicates whether output should be included.
	 */
	private boolean readBody(boolean include) throws IOException
	{
		while (line != null)
		{
			String trimmed = line.trim();
			
			if (trimmed.startsWith("#"))
			{
				String[] parts = trimmed.split("\\s+");
				
				switch (parts[0])
				{
					case "#ifdef":
						readIfdef(parts, include);
						break;
						
					case "#ifndef":
						readIfndef(parts, include);
						break;
	
					case "#define":
						readDefine(parts, include);
						break;
						
					case "#undef":
						readUndef(parts, include);
						break;
						
					case "#elseif":
					case "#endif":
					case "#else":
						return include;		// line still has #...
						
					default:
						writeLine(include);
				}
			}
			else
			{
				writeLine(include);
			}
		}
		
		return include;
	}

	private void readIfdef(String[] ifdef, boolean include) throws IOException
	{
		if (ifdef.length >= 2)
		{
			readIf(System.getProperty(ifdef[1]) != null, include);
		}
		else
		{
			error(1502, "Expecting #ifdef <property>", "#ifdef");
			readIf(true, include);
		}
	}

	private void readIfndef(String[] ifndef, boolean include) throws IOException
	{
		if (ifndef.length >= 2)
		{
			readIf(System.getProperty(ifndef[1]) == null, include);
		}
		else
		{
			error(1503, "Expecting #ifndef <property>", "#ifndef");
			readIf(true, include);
		}
	}

	private void readIf(boolean condition, boolean include) throws IOException
	{
		writeLine(false);	// #if?def line
		
		boolean done = readBody(include && condition);
		
		while (line != null)
		{
			String trimmed = line.trim();
			
			if (trimmed.startsWith("#"))
			{
				String[] parts = trimmed.split("\\s+");

				switch (parts[0])
				{
					case "#else":
						writeLine(false);		// #else line
						readBody(include && !done);
						break;
						
					case "#endif":
						writeLine(false);		// #endif line
						return;
						
					case "#elseif":
						writeLine(false);		// #elseif line
						done |= readBody(include && !done && System.getProperty(parts[1]) != null);
						break;
						
					default:
						writeLine(include);
				}
			}
			else
			{
				writeLine(include);				
			}
		}
	}

	private void readDefine(String[] parts, boolean include) throws IOException
	{
		if (parts.length < 2)
		{
			error(1504, "Expecting #define <property> [value]", "#define");
		}
		else if (include)
		{
			String name = parts[1];
			
			if (parts.length == 2)	// eg. #define NAME
			{
				System.setProperty(name, "1");
			}
			else
			{
				int pos = line.indexOf(name) + name.length();	// eg. #define X "abc def"
				String value = line.substring(pos).trim();
				System.setProperty(name, value);
				
				if (name.startsWith("vdmj."))
				{
					Properties.init();
				}
			}
		}

		writeLine(false);	// #define line
	}

	private void readUndef(String[] parts, boolean include) throws IOException
	{
		if (parts.length < 2)
		{
			error(1505, "Expecting #undef <property>", "#undef");
		}
		else if (include)
		{
			String name = parts[1];
			System.clearProperty(name);
			
			if (name.startsWith("vdmj."))
			{
				Properties.init();
			}
		}

		writeLine(false);	// #undef line
	}

	public List<VDMError> getErrors()
	{
		return errs;
	}
	
	public int getErrorCount()
	{
		return errs.size();
	}

}