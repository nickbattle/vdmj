/*******************************************************************************
 *
 *	Copyright (c) 2023 Fujitsu Services Ltd.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fujitsu.vdmj.plugins.PluginRegistry;

public class PluginTest extends VDMJUnitTestSL
{
	@BeforeClass
	public static void start() throws Exception
	{
		// Note: vdmj.plugins adds com.fujitsu.vdmjunit.TestPlugin
		readSpecification("SL");	// Includes two files
	}
	
	@AfterClass
	public static void stop()
	{
		// Nothing
	}
	
	@Before
	public void setUp()
	{
		init();
	}
	
	@Test
	public void one() throws Exception
	{
		TestPlugin plugin = PluginRegistry.getInstance().getPlugin("Test");
		assertTrue(plugin != null);
		assertEquals(2, plugin.getCount());		// Set when processing events
	}
}
