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
 *
 ******************************************************************************/

package vdmjunit;

import java.nio.charset.Charset;

import org.overturetool.vdmj.lex.Dialect;
import org.overturetool.vdmj.runtime.ClassInterpreter;

/**
 * The VDMJUnit class for testing VDM-RT specifications.
 */
abstract public class VDMJUnitTestRT extends VDMJUnitTest
{
	/**
	 * @see vdmjunit.VDMJUnitTest#readSpecification(String...)
	 */
	protected static void readSpecification(String... files) throws Exception
	{
		readSpecification(Charset.defaultCharset(), files);
	}

	/**
	 * @see vdmjunit.VDMJUnitTest#readSpecification(Charset, String...)
	 */
	protected static void readSpecification(Charset charset, String... files) throws Exception
	{
		SpecificationReader reader = new OOSpecificationReader(Dialect.VDM_RT);
		interpreter = reader.readSpecification(charset, files);
	}

	/**
	 * @see vdmjunit.VDMJUnitTest#create(String, String)
	 */
	protected void create(String name, String value) throws Exception
	{
		ClassInterpreter ci = (ClassInterpreter) interpreter;
		ci.create(name, value);
	}
}
