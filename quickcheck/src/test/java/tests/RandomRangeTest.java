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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFieldList;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.values.CPUValue;
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
	
	@Test
	public void testRecord()
	{
		Context ctxt = new Context(LexLocation.ANY, "Test Context", null);
		ctxt.setThreadState(CPUValue.vCPU);
		TCNameToken name = new TCNameToken(LexLocation.ANY, "DEFAULT", "R");
		TCFieldList fields = new TCFieldList();
		fields.add(new TCField(new TCNameToken(LexLocation.ANY, "DEFAULT", "a"), "a", new TCIntegerType(LexLocation.ANY), false));
		fields.add(new TCField(new TCNameToken(LexLocation.ANY, "DEFAULT", "b"), "b", new TCIntegerType(LexLocation.ANY), false));
		TCRecordType type = new TCRecordType(name, fields, false);
		ValueSet result = type.apply(new RandomRangeCreator(ctxt, 1234), 10);
		assertTrue(10 >= result.size());
		result = type.apply(new RandomRangeCreator(ctxt, 1234), 50);
		assertTrue(50 >= result.size());
	}
	
	@Test
	public void testSet()
	{
		TCSetType type = new TCSetType(LexLocation.ANY, new TCIntegerType(LexLocation.ANY));
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 10);
		assertEquals(10, result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertEquals(1001, result.size());
	}
	
	@Test
	public void testSet1()
	{
		TCSet1Type type = new TCSet1Type(LexLocation.ANY, new TCIntegerType(LexLocation.ANY));
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 10);
		assertEquals(10, result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertEquals(1001, result.size());
	}
	
	@Test
	public void testSeq()
	{
		TCSeqType type = new TCSeqType(LexLocation.ANY, new TCIntegerType(LexLocation.ANY));
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 10);
		assertEquals(10, result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertEquals(1001, result.size());
	}
	
	@Test
	public void testSeq1()
	{
		TCSeq1Type type = new TCSeq1Type(LexLocation.ANY, new TCIntegerType(LexLocation.ANY));
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 10);
		assertEquals(10, result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertEquals(1001, result.size());
	}
	
	@Test
	public void testMap()
	{
		TCMapType type = new TCMapType(LexLocation.ANY, new TCIntegerType(LexLocation.ANY), new TCIntegerType(LexLocation.ANY));
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 10);
		assertTrue(10 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertTrue(1001 >= result.size());
	}
	
	@Test
	public void testProduct()
	{
		TCTypeList types = new TCTypeList();
		types.add(new TCIntegerType(LexLocation.ANY));
		types.add(new TCIntegerType(LexLocation.ANY));
		TCProductType type = new TCProductType(LexLocation.ANY, types);
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 10);
		assertTrue(10 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertTrue(1001 >= result.size());
	}
	
	@Test
	public void testUnion()
	{
		TCTypeSet types = new TCTypeSet();
		types.add(new TCIntegerType(LexLocation.ANY));
		types.add(new TCBooleanType(LexLocation.ANY));
		TCUnionType type = new TCUnionType(LexLocation.ANY, types);
		ValueSet result = type.apply(new RandomRangeCreator(null, 1234), 10);
		assertTrue(10 >= result.size());
		result = type.apply(new RandomRangeCreator(null, 1234), 1001);
		assertTrue(1001 >= result.size());
	}
}
