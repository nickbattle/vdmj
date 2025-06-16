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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmjunit;

import static org.junit.Assert.fail;

import java.nio.charset.Charset;
import java.util.List;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.plugins.EventHub;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.runtime.Interpreter;

/**
 * The abstract parent class of all specification readers.
 */
abstract public class SpecificationReader
{
	private VDMJUnitLifecycle lifecycle = null;

	/**
	 * Construct a SpecificationReader for a particular VDM dialect.
	 * 
	 * @param dialect
	 */
	public SpecificationReader(Dialect dialect)
	{
		Properties.init();
		Settings.dialect = dialect;
		PluginRegistry.reset();
		EventHub.reset();
		
		lifecycle = new VDMJUnitLifecycle("-i");	// Because we want an interpreter
		lifecycle.loadPlugins();
		lifecycle.processArgs();
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
		lifecycle.findFiles(charset, filenames);
		
		if (!lifecycle.checkAndInitFiles())
		{
			fail("Type errors (see stdout)");
		}

		return Interpreter.getInstance();
	}

	/**
	 * Return the syntax and type checking errors from the last readSpecification
	 * @return a list of errors
	 */
	public List<VDMError> getErrors()
	{
		return lifecycle.errors;
	}

	/**
	 * Return the warnings from the last readSpecification
	 * @return a list of warnings
	 */
	public List<VDMWarning> getWarnings()
	{
		return lifecycle.warnings;
	}
}
