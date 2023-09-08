/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package tests;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.values.ValueSet;

import quickcheck.visitors.RandomRangeCreator;

public class RandomRangeTest
{
	@Test
	public void testCharacter()
	{
		TCCharacterType type = new TCCharacterType(LexLocation.ANY);
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 2);
		assertTrue(2 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 5);
		assertTrue(5 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertTrue(26 >= result.size());
	}

	@Test
	public void testToken()
	{
		TCTokenType type = new TCTokenType(LexLocation.ANY);
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 2);
		assertTrue(2 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 5);
		assertTrue(5 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertTrue(1001 >= result.size());
	}

	@Test
	public void testNatural()
	{
		TCNaturalType type = new TCNaturalType(LexLocation.ANY);
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 2);
		assertTrue(2 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 5);
		assertTrue(5 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertTrue(1001 >= result.size());
	}

	@Test
	public void testInteger()
	{
		TCIntegerType type = new TCIntegerType(LexLocation.ANY);
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 2);
		assertTrue(2 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 5);
		assertTrue(5 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertTrue(1001 >= result.size());
	}

	@Test
	public void testReal()
	{
		TCRealType type = new TCRealType(LexLocation.ANY);
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 2);
		assertTrue(2 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 5);
		assertTrue(5 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertTrue(1001 >= result.size());
	}
}
