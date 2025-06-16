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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import com.fujitsu.vdmjunit.VDMJUnitTestPP;

public class ErrorTest extends VDMJUnitTestPP
{
	@Test
	public void warning() throws Exception
	{
		readSpecification("warning.vpp");
		assertTrue(getErrors().isEmpty());
		assertTrue(getWarnings().size() == 1);
	}
	
	@Test
	public void tcerror() throws Exception
	{
		try
		{
			readSpecification("tcerror.vpp");
			fail("Expecting read to fail!");
		}
		catch (AssertionError e)
		{
			assertTrue(getErrors().size() == 2);
			assertTrue(getWarnings().isEmpty());
		}
	}
}
