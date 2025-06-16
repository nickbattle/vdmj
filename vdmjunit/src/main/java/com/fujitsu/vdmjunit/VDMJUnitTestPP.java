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

import java.nio.charset.Charset;

import org.junit.AfterClass;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.scheduler.SchedulableThread;

/**
 * The VDMJUnit class for testing VDM++ specifications.
 */
abstract public class VDMJUnitTestPP extends VDMJUnitTest
{
	/**
	 * @see com.fujitsu.vdmjunit.VDMJUnitTest#readSpecification(String...)
	 */
	protected static void readSpecification(String... files) throws Exception
	{
		readSpecification(Charset.defaultCharset(), files);
	}

	/**
	 * @see com.fujitsu.vdmjunit.VDMJUnitTest#readSpecification(Charset, String...)
	 */
	protected static void readSpecification(Charset charset, String... files) throws Exception
	{
		reader = new OOSpecificationReader(Dialect.VDM_PP);
		interpreter = reader.readSpecification(charset, files);
	}

	/**
	 * @see com.fujitsu.vdmjunit.VDMJUnitTest#create(String, String)
	 */
	@Override
	protected void create(String name, String value) throws Exception
	{
		ClassInterpreter ci = (ClassInterpreter) interpreter;
		ci.create(name, value);
	}
	
	/**
	 * Clean up all threads for PP and RT because each JUnit is one "session".
	 */
	@AfterClass
	public static void afterClass()
	{
		SchedulableThread.terminateAll();
	}
}
