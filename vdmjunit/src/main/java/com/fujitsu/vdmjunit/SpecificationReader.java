/*******************************************************************************
 *
 *	Copyright (c) 2013 Fujitsu Services Ltd.
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

package com.fujitsu.vdmjunit;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.util.GetResource;

/**
 * The abstract parent class of all specification readers.
 */
abstract public class SpecificationReader
{
	protected List<VDMError> errors = new Vector<VDMError>();
	protected List<VDMWarning> warnings = new Vector<VDMWarning>();

	/**
	 * Construct a SpecificationReader for a particular VDM dialect.
	 * 
	 * @param dialect
	 */
	public SpecificationReader(Dialect dialect)
	{
		Settings.dialect = dialect;
	}
	
	/**
	 * Print a list of error messages to stdout. This is used internally by the
	 * readSpecification method to report syntax or type checking errors before
	 * stopping.
	 * 
	 * @param messages A list of messages to print.
	 */
	protected void printMessages(List<? extends VDMMessage> messages)
	{
		for (VDMMessage message: messages)
		{
			System.out.println(message);
		}
	}
	
	/**
	 * Parse and type check source files, and create an interpreter. Directories are
	 * expanded to include any VDM source files they contain.
	 * 
	 * @param charset The charset for the specification files.
	 * @param filenames A list of VDM source files or directories.
	 * @throws Exception 
	 */
	public Interpreter readSpecification(Charset charset, String... filenames) throws Exception
	{
		List<File> list = new Vector<File>(filenames.length);
		
		for (String filename: filenames)
		{
			URL url = getClass().getResource("/" + filename);
			
			if (url == null)
			{
				throw new FileNotFoundException(filename);
			}
			
			File file = null;
			
			if (url.getProtocol().equals("jar"))
			{
				file = GetResource.load(new File("/" + filename));
			}
			else
			{
				file = new File(url.toURI());
			}
			
			if (file.isDirectory())
			{
				for (File subfile: file.listFiles(Settings.dialect.getFilter()))
				{
					if (subfile.isFile())
					{
						list.add(subfile);
					}
				}
			}
			else
			{
				list.add(file);
			}
		}
		
		return readSpecification(charset, list);
	}

	/**
	 * Parse and type check the supplied list of Files, and return an interpreter.
	 * 
	 * @param charset The character encoding for the files.
	 * @param list A list of Files.
	 * @return An Interpreter instance
	 * @throws Exception
	 */
	protected abstract Interpreter readSpecification(Charset charset, List<File> list) throws Exception;

	/**
	 * Return the syntax and type checking errors from the last readSpecification
	 * @return a list of errors
	 */
	public List<VDMError> getErrors()
	{
		return errors;
	}

	/**
	 * Return the warnings from the last readSpecification
	 * @return a list of warnings
	 */
	public List<VDMWarning> getWarnings()
	{
		return warnings;
	}
}
