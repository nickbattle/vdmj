/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.junit;

import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INForAllExpression;
import com.fujitsu.vdmj.in.expressions.INImpliesExpression;
import com.fujitsu.vdmj.in.expressions.INNotExpression;
import com.fujitsu.vdmj.in.expressions.INOrExpression;
import com.fujitsu.vdmj.in.expressions.INUndefinedExpression;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.in.patterns.INMultipleTypeBind;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.expressions.INAndExpression;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INEquivalentExpression;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CPUValue;

import junit.framework.TestCase;

public class UndefinedTest extends TestCase
{
	private final INExpression TRUE = new INBooleanLiteralExpression(new LexBooleanToken(true, LexLocation.ANY));
	private final INExpression FALSE = new INBooleanLiteralExpression(new LexBooleanToken(false, LexLocation.ANY));
	private final INExpression UNDEFINED = new INUndefinedExpression(LexLocation.ANY);

	private final LexKeywordToken AND = new LexKeywordToken(Token.AND, LexLocation.ANY);
	private final LexKeywordToken OR = new LexKeywordToken(Token.OR, LexLocation.ANY);
	private final LexKeywordToken IMP = new LexKeywordToken(Token.IMPLIES, LexLocation.ANY);
	private final LexKeywordToken EQV = new LexKeywordToken(Token.EQUIVALENT, LexLocation.ANY);

	private final Value T = new BooleanValue(true);
	private final Value F = new BooleanValue(false);

	private Context ctxt = null;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();

		ctxt = new Context(LexLocation.ANY, "test context", null);
		ctxt.setThreadState(CPUValue.vCPU);
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	public void testAnd() throws Exception
	{
		assertTrue(new INAndExpression(TRUE, AND, UNDEFINED).eval(ctxt).isUndefined());
		assertTrue(new INAndExpression(FALSE, AND, UNDEFINED).eval(ctxt).equals(F));
		assertTrue(new INAndExpression(UNDEFINED, AND, TRUE).eval(ctxt).isUndefined());
		assertTrue(new INAndExpression(UNDEFINED, AND, FALSE).eval(ctxt).equals(F));
		assertTrue(new INAndExpression(UNDEFINED, AND, UNDEFINED).eval(ctxt).isUndefined());
	}

	public void testOr() throws Exception
	{
		assertTrue(new INOrExpression(TRUE, OR, UNDEFINED).eval(ctxt).equals(T));
		assertTrue(new INOrExpression(FALSE, OR, UNDEFINED).eval(ctxt).isUndefined());
		assertTrue(new INOrExpression(UNDEFINED, OR, TRUE).eval(ctxt).equals(T));
		assertTrue(new INOrExpression(UNDEFINED, OR, FALSE).eval(ctxt).isUndefined());
		assertTrue(new INOrExpression(UNDEFINED, OR, UNDEFINED).eval(ctxt).isUndefined());
	}

	public void testNot() throws Exception
	{
		assertTrue(new INNotExpression(LexLocation.ANY, UNDEFINED).eval(ctxt).isUndefined());
	}

	public void testImplies() throws Exception
	{
		assertTrue(new INImpliesExpression(TRUE, IMP, UNDEFINED).eval(ctxt).isUndefined());
		assertTrue(new INImpliesExpression(FALSE, IMP, UNDEFINED).eval(ctxt).equals(T));
		assertTrue(new INImpliesExpression(UNDEFINED, IMP, TRUE).eval(ctxt).equals(T));
		assertTrue(new INImpliesExpression(UNDEFINED, IMP, FALSE).eval(ctxt).isUndefined());
		assertTrue(new INImpliesExpression(UNDEFINED, IMP, UNDEFINED).eval(ctxt).isUndefined());
	}

	public void testEquiv() throws Exception
	{
		assertTrue(new INEquivalentExpression(TRUE, EQV, UNDEFINED).eval(ctxt).isUndefined());
		assertTrue(new INEquivalentExpression(FALSE, EQV, UNDEFINED).eval(ctxt).isUndefined());
		assertTrue(new INEquivalentExpression(UNDEFINED, EQV, TRUE).eval(ctxt).isUndefined());
		assertTrue(new INEquivalentExpression(UNDEFINED, EQV, FALSE).eval(ctxt).isUndefined());
		assertTrue(new INEquivalentExpression(UNDEFINED, EQV, UNDEFINED).eval(ctxt).isUndefined());
	}
}