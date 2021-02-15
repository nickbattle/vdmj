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

import java.math.BigInteger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fujitsu.vdmjunit.VDMJUnitTestRT;

public class RTTest extends VDMJUnitTestRT
{
	@BeforeClass
	public static void start() throws Exception
	{
		readSpecification("testRT.vpp");
	}
	
	@Before
	public void setUp()
	{
		init();
	}
	
	@Test
	public void one() throws Exception
	{
		assertEquals(new BigInteger("321"), runInt("obj.Get2(obj2)"));
	}
}
