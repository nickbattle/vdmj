/*******************************************************************************
 *
 *	Copyright (c) 2024 Nick Battle.
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

package com.fujitsu.vdmj.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.fujitsu.vdmj.mapper.extra.TestExtra;

/**
 * Basic tests of the ClassMapper.
 */
public class ClassMapperTest
{
	@Test
	public void test()
	{
		// Properties.mapping_search_path = "/mapper";
		// Also uses the vdmj.mappings resource to add extra search paths
		ClassMapper mapper = ClassMapper.getInstance("test.mappings");
		assertEquals(0, mapper.getNodeCount());
		
		try
		{
			TestSource source = new TestSource("top", new TestSource("left"), new TestExtra("right"));
			TestDestination dest = mapper.convert(source);
			assertEquals("top[left[null,null],Extra:right[null,null]]", dest.toString());
		}
		catch (Exception e)
		{
			fail("Failed with " + e);
		}
	}
}
