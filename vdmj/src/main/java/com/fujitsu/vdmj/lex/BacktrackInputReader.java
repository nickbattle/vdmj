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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.lex;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.util.GetResource;

/**
 * A class to allow arbitrary checkpoints and backtracking while
 * parsing a file.
 */
public class BacktrackInputReader
{
	/** A stack of position markers for popping. */
	private final Stack<Integer> stack = new Stack<Integer>();

	/** The characters from the file. */
	private final char[] data;

	/** The current read position. */
	private int pos = 0;

	/** External readers */
	private static Map<String, Class<? extends ExternalFormatReader>> externalReaders = null;
	
	/** Ifdef processing */
	private final IfdefProcessor ifdefProcessor;
	
	/**
	 * Create an object to read the file name passed with the given charset.
	 *
	 * @param file	The filename to open
	 * @param charset The encoding of the file.
	 */
	public BacktrackInputReader(File file, Charset charset)
	{
		try
		{
			ifdefProcessor = new IfdefProcessor(file);
			ExternalFormatReader efr = readerFactory(file);
			char[] source = efr.getText(file, charset);
			data = ifdefProcessor.getText(source);
			pos = 0;
		}
		catch (Exception e)
		{
			throw new InternalException(0, "Cannot read file: " + e.getMessage());
		}
	}

	/**
	 * Create an object to read the string passed. This is used in the
	 * interpreter to parse expressions typed in.
	 *
	 * @param file The filename of the content, for errors
	 * @param content The actual content.
	 */
	public BacktrackInputReader(File file, String content)
	{
		try
		{
			ifdefProcessor = new IfdefProcessor(file);
			LatexStreamReader lsr = new LatexStreamReader();
			char[] source = lsr.getText(content);
			data = ifdefProcessor.getText(source);
			pos = 0;
		}
		catch (IOException e)
		{
			throw new InternalException(0, "Cannot read file: " + e.getMessage());
		}
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
	public static ExternalFormatReader readerFactory(File file) throws Exception
	{
		String lowerName = file.getName().toLowerCase();		// NB! lower case matched
		
		if (externalReaders == null)
		{
			buildExternalReaders();
		}
		
		for (String suffix: externalReaders.keySet())
		{
			if (lowerName.endsWith(suffix))
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
	 * Property format is "<suffix>=<class>,&lt;suffix&gt;=&lt;class&gt;,..."
	 */
	@SuppressWarnings("unchecked")
	private static synchronized void buildExternalReaders() throws Exception
	{
		externalReaders = new HashMap<String, Class<? extends ExternalFormatReader>>();
		
		// Add the standard readers first
		externalReaders.put(".tex", LatexStreamReader.class);	// To allow *.tex files
		externalReaders.put(".latex", LatexStreamReader.class);	// To allow *.latex files
		externalReaders.put(".doc", DocStreamReader.class);
		externalReaders.put(".docx", DocxStreamReader.class);
		externalReaders.put(".odt", ODFStreamReader.class);
		externalReaders.put(".adoc", AsciiDocStreamReader.class);
		externalReaders.put(".md", MarkdownStreamReader.class);
		externalReaders.put(".markdown", MarkdownStreamReader.class);
		
		List<String> userExtReaders = GetResource.readResource("vdmj.readers");
		
		if (!userExtReaders.isEmpty())
		{
			for (String readerPair: userExtReaders)
			{
				try
				{
					String[] parts = readerPair.split("\\s*=\\s*");
					
					if (parts.length == 2)
					{
						Class<? extends ExternalFormatReader> clazz = (Class<? extends ExternalFormatReader>) Class.forName(parts[1]);
						externalReaders.put(parts[0].toLowerCase(), clazz);
					}
					else
					{
						System.err.printf("Malformed external readers resource?\n");
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
			catch (Exception e)
			{
				return false;
			}
		}
		
		for (String key: externalReaders.keySet())
		{
			if (file.getName().toLowerCase().endsWith(key))
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
	
	/**
	 * Get any IfdefProcessor errors.
	 */
	public List<VDMError> getErrors()
	{
		return ifdefProcessor.getErrors();
	}
	
	public int getErrorCount()
	{
		return ifdefProcessor.getErrorCount();
	}
}
