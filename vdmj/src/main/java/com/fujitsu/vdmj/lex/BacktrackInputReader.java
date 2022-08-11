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

package com.fujitsu.vdmj.lex;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.messages.InternalException;

/**
 * A class to allow arbitrary checkpoints and backtracking while
 * parsing a file.
 */
public class BacktrackInputReader
{
	/** A stack of position markers for popping. */
	private Stack<Integer> stack = new Stack<Integer>();

	/** The characters from the file. */
	private char[] data;

	/** The current read position. */
	private int pos = 0;

	/** External readers */
	private static Map<String, Class<? extends ExternalFormatReader>> externalReaders = null;
	
	/**
	 * Create an object to read the file name passed with the given charset.
	 *
	 * @param file	The filename to open
	 */
	public BacktrackInputReader(File file, String charset)
	{
		try
		{
			ExternalFormatReader efr = readerFactory(file, charset);
			data = efr.getText(file, charset);
			pos = 0;
		}
		catch (IOException e)
		{
			throw new InternalException(0, "Cannot read file: " + e.getMessage());
		}
	}

	/**
	 * Create an object to read the file name passed with the default charset.
	 *
	 * @param file	The filename to open
	 */
	public BacktrackInputReader(File file)
	{
		this(file, Charset.defaultCharset().name());
	}

	/**
	 * Create an object to read the string passed. This is used in the
	 * interpreter to parse expressions typed in.
	 *
	 * @param expression
	 */
	public BacktrackInputReader(String expression, String charset)
	{
		try
		{
			LatexStreamReader lsr = new LatexStreamReader();
			data = lsr.getText(expression);
			pos = 0;
		}
		catch (IOException e)
		{
			throw new InternalException(0, "Cannot read file: " + e.getMessage());
		}
	}
	
	/**
	 * Create an object to read the string passed with the default charset.
	 */
	public BacktrackInputReader(String expression)
	{
		this(expression, Charset.defaultCharset().name());
	}

	/**
	 * Return the entire content of the file.
	 */
	public char[] getText()
	{
		return data;
	}

	/**
	 * Create an ExternalFormatReader from a File, depending on the filename.
	 * @throws IOException 
	 */
	private ExternalFormatReader readerFactory(File file, String charset) throws IOException
	{
		String name = file.getName();
		
		if (externalReaders == null)
		{
			buildExternalReaders();
		}
		
		for (String suffix: externalReaders.keySet())
		{
			if (name.endsWith(suffix))
			{
				try
				{
					Class<? extends ExternalFormatReader> clazz = externalReaders.get(suffix);
					return clazz.getDeclaredConstructor().newInstance();
				}
				catch (Exception e)
				{
					throw new IOException("External reader failed: " + e);
				}
			}
		}

		return new LatexStreamReader();		// Reader of last resort :-)
	}

	/**
	 * Property format is "<suffix>=<class>,<suffix>=<class>,..."
	 */
	@SuppressWarnings("unchecked")
	private static synchronized void buildExternalReaders() throws IOException
	{
		externalReaders = new HashMap<String, Class<? extends ExternalFormatReader>>();
		
		// Add the standard readers first
		externalReaders.put(".doc", DocStreamReader.class);
		externalReaders.put(".DOC", DocStreamReader.class);
		externalReaders.put(".docx", DocxStreamReader.class);
		externalReaders.put(".DOCX", DocxStreamReader.class);
		externalReaders.put(".odt", ODFStreamReader.class);
		externalReaders.put(".ODT", ODFStreamReader.class);
		externalReaders.put(".adoc", AsciiDocStreamReader.class);
		externalReaders.put(".ADOC", AsciiDocStreamReader.class);
		externalReaders.put(".md", MarkdownStreamReader.class);
		externalReaders.put(".markdown", MarkdownStreamReader.class);
		
		if (Properties.parser_external_readers != null)
		{
			String[] readers = Properties.parser_external_readers.split("\\s*,\\s*");
			
			for (String readerPair: readers)
			{
				try
				{
					String[] parts = readerPair.split("\\s*=\\s*");
					
					if (parts.length == 2)
					{
						Class<? extends ExternalFormatReader> clazz = (Class<? extends ExternalFormatReader>) Class.forName(parts[1]);
						externalReaders.put(parts[0], clazz);
					}
					else
					{
						System.err.printf("Malformed external readers: %s\n", Properties.parser_external_readers);
					}
				}
				catch (Exception e)
				{
					throw new IOException("Build external readers failed: " + e);
				}
			}
		}
	}

	/**
	 * Test whether an external format reader is used for File.
	 */
	public static boolean isExternalFormat(File file)
	{
		if (externalReaders == null)
		{
			try
			{
				buildExternalReaders();
			}
			catch (IOException e)
			{
				return false;
			}
		}
		
		for (String key: externalReaders.keySet())
		{
			if (file.getName().endsWith(key))
			{
				return true;
			}
		}
		
		return false;	// use LaTeX reader
	}


	/**
	 * Push the current location to permit backtracking.
	 */
	public void push()
	{
		stack.push(pos);
	}

	/**
	 * Pop the last location, but do not backtrack to it. This is used when
	 * the parser reached a point where a potential ambiguity has been resolved,
	 * and it knows that it will never need to backtrack.
	 */
	public void unpush()
	{
		stack.pop();	// don't set pos though
	}

	/**
	 * Pop the last location and reposition the stream at that position. The
	 * state of the stream is such that the next read operation will return
	 * the same character that would have been read immediately after the
	 * push() operation that saved the position.
	 */
	public void pop()
	{
		pos = stack.pop();
	}

	/**
	 * Read one character.
	 */
	public char readCh()
	{
		return (pos == data.length) ? (char)-1 : data[pos++];
	}
}
