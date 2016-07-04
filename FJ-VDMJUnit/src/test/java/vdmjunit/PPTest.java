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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.overturetool.vdmj.runtime.ContextException;

import vdmjunit.VDMJUnitTestPP;

public class PPTest extends VDMJUnitTestPP
{
	@BeforeClass
	public static void start() throws Exception
	{
		readSpecification("testPP.vpp");
	}
	
	@Before
	public void setUp()
	{
		init();
	}
	
	@Test
	public void one() throws Exception
	{
		create("object", "new A()");
		assertEquals(new BigInteger("2"), runInt("object.f(1)"));
	}
	
	@Test
	public void two() throws Exception
	{
		create("object", "new A()");
		assertEquals(new BigInteger("123"), runInt("object.getValue()"));
	}
	
	@Test
	public void three() throws Exception
	{
		try
		{
			create("object", "new A()");
			run("object.f(100)");
			fail("Expecting precondition failure");
		}
		catch (ContextException e)
		{
			// Error 4055: Precondition failure: pre_f in 'A' (test.vpp) at line 8:11
			assertEquals(4055, e.number);
			assertEquals("A", e.location.module);
			assertEquals("testPP.vpp", e.location.file.getName());
			assertEquals(8, e.location.startLine);
			assertEquals(11, e.location.startPos);
		}
	}
}
