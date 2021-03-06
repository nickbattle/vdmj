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
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fujitsu.vdmj.values.CharacterValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmjunit.VDMJUnitTestPP;

public class UTF8Test extends VDMJUnitTestPP
{
	@BeforeClass
	public static void start() throws Exception
	{
		readSpecification(Charset.forName("UTF8"), "dvorak.vpp");
	}
	
	@Before
	public void setUp()
	{
		init();
	}
	
	@Test
	public void one() throws Exception
	{
		Value ch = run("euro()");
		assertTrue(ch instanceof CharacterValue);
		CharacterValue chv = (CharacterValue)ch;
		assertEquals('\u20AC', chv.unicode);
	}
}
