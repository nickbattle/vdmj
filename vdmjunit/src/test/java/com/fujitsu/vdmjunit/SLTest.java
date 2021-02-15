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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmjunit.VDMJUnitTestSL;

public class SLTest extends VDMJUnitTestSL
{
	@BeforeClass
	public static void start() throws Exception
	{
		readSpecification("SL");
	}
	
	@Before
	public void setUp()
	{
		init();
	}
	
	@Test
	public void one() throws Exception
	{
		setDefault("A");
		run("setValue(123)");
		assertEquals(new BigInteger("123"), runInt("getValue()"));
		assertVDM("getValue()", "RESULT = 123");
		
		try
		{
			assertVDM("Testing!", "getValue()", "RESULT=456");
			fail("Expected failure");
		}
		catch (AssertionError e)
		{
			assertEquals(e.getMessage(), "Testing!");
		}
	}
	
	@Test
	public void two() throws Exception
	{
		assertEquals(new BigInteger("100"), runInt("B`g(99)"));
		setDefault("B");
		Value r = run("g(1)");
		assertEquals(new BigInteger("2"), r.intValue(null));
		assertVDM(r, "RESULT = 2");
		
		try
		{
			assertVDM("Testing!", r, "RESULT=456");
			fail("Expected failure");
		}
		catch (AssertionError e)
		{
			assertEquals(e.getMessage(), "Testing!");
		}
	}
	
	@Test
	public void three() throws Exception
	{
		assertEquals(true, runBool("100 > 10"));
	}
}
