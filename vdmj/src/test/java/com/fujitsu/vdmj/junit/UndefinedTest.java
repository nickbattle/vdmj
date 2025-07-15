/*******************************************************************************
 *
 *	Copyright (c) 2025 Fujitsu Services Ltd.
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
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INForAllExpression;
import com.fujitsu.vdmj.in.expressions.INImpliesExpression;
import com.fujitsu.vdmj.in.expressions.INNotExpression;
import com.fujitsu.vdmj.in.expressions.INOrExpression;
import com.fujitsu.vdmj.in.expressions.INSetEnumExpression;
import com.fujitsu.vdmj.in.expressions.INUndefinedExpression;
import com.fujitsu.vdmj.in.expressions.INVariableExpression;
import com.fujitsu.vdmj.in.patterns.INIdentifierPattern;
import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.in.patterns.INMultipleSetBind;
import com.fujitsu.vdmj.in.patterns.INMultipleTypeBind;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.expressions.INAndExpression;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INEquivalentExpression;
import com.fujitsu.vdmj.in.expressions.INExistsExpression;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CPUValue;

import junit.framework.TestCase;

public class UndefinedTest extends TestCase
{
	private final INExpression TRUE = new INBooleanLiteralExpression(LexBooleanToken.TRUE);
	private final INExpression FALSE = new INBooleanLiteralExpression(LexBooleanToken.FALSE);
	private final INExpression UNDEFINED = new INUndefinedExpression(LexLocation.ANY);

	private final LexKeywordToken AND = new LexKeywordToken(Token.AND, LexLocation.ANY);
	private final LexKeywordToken OR  = new LexKeywordToken(Token.OR, LexLocation.ANY);
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

		assertTrue(new INAndExpression(TRUE, AND, TRUE).eval(ctxt).equals(T));
		assertTrue(new INAndExpression(TRUE, AND, FALSE).eval(ctxt).equals(F));
		assertTrue(new INAndExpression(FALSE, AND, TRUE).eval(ctxt).equals(F));
		assertTrue(new INAndExpression(FALSE, AND, FALSE).eval(ctxt).equals(F));
	}

	public void testOr() throws Exception
	{
		assertTrue(new INOrExpression(TRUE, OR, UNDEFINED).eval(ctxt).equals(T));
		assertTrue(new INOrExpression(FALSE, OR, UNDEFINED).eval(ctxt).isUndefined());
		assertTrue(new INOrExpression(UNDEFINED, OR, TRUE).eval(ctxt).equals(T));
		assertTrue(new INOrExpression(UNDEFINED, OR, FALSE).eval(ctxt).isUndefined());
		assertTrue(new INOrExpression(UNDEFINED, OR, UNDEFINED).eval(ctxt).isUndefined());

		assertTrue(new INOrExpression(TRUE, OR, TRUE).eval(ctxt).equals(T));
		assertTrue(new INOrExpression(TRUE, OR, FALSE).eval(ctxt).equals(T));
		assertTrue(new INOrExpression(FALSE, OR, TRUE).eval(ctxt).equals(T));
		assertTrue(new INOrExpression(FALSE, OR, FALSE).eval(ctxt).equals(F));
	}

	public void testNot() throws Exception
	{
		assertTrue(new INNotExpression(LexLocation.ANY, UNDEFINED).eval(ctxt).isUndefined());

		assertTrue(new INNotExpression(LexLocation.ANY, TRUE).eval(ctxt).equals(F));
		assertTrue(new INNotExpression(LexLocation.ANY, FALSE).eval(ctxt).equals(T));
	}

	public void testImplies() throws Exception
	{
		assertTrue(new INImpliesExpression(TRUE, IMP, UNDEFINED).eval(ctxt).isUndefined());
		assertTrue(new INImpliesExpression(FALSE, IMP, UNDEFINED).eval(ctxt).equals(T));
		assertTrue(new INImpliesExpression(UNDEFINED, IMP, TRUE).eval(ctxt).equals(T));
		assertTrue(new INImpliesExpression(UNDEFINED, IMP, FALSE).eval(ctxt).isUndefined());
		assertTrue(new INImpliesExpression(UNDEFINED, IMP, UNDEFINED).eval(ctxt).isUndefined());

		assertTrue(new INImpliesExpression(TRUE, IMP, TRUE).eval(ctxt).equals(T));
		assertTrue(new INImpliesExpression(TRUE, IMP, FALSE).eval(ctxt).equals(F));
		assertTrue(new INImpliesExpression(FALSE, IMP, TRUE).eval(ctxt).equals(T));
		assertTrue(new INImpliesExpression(FALSE, IMP, FALSE).eval(ctxt).equals(T));
	}

	public void testEquiv() throws Exception
	{
		assertTrue(new INEquivalentExpression(TRUE, EQV, UNDEFINED).eval(ctxt).isUndefined());
		assertTrue(new INEquivalentExpression(FALSE, EQV, UNDEFINED).eval(ctxt).isUndefined());
		assertTrue(new INEquivalentExpression(UNDEFINED, EQV, TRUE).eval(ctxt).isUndefined());
		assertTrue(new INEquivalentExpression(UNDEFINED, EQV, FALSE).eval(ctxt).isUndefined());
		assertTrue(new INEquivalentExpression(UNDEFINED, EQV, UNDEFINED).eval(ctxt).isUndefined());

		assertTrue(new INEquivalentExpression(TRUE, EQV, TRUE).eval(ctxt).equals(T));
		assertTrue(new INEquivalentExpression(TRUE, EQV, FALSE).eval(ctxt).equals(F));
		assertTrue(new INEquivalentExpression(FALSE, EQV, TRUE).eval(ctxt).equals(F));
		assertTrue(new INEquivalentExpression(FALSE, EQV, FALSE).eval(ctxt).equals(T));
	}

	public void testForall() throws Exception
	{
		TCNameToken VAR = new TCNameToken(LexLocation.ANY, "DEFAULT", "x");
		INPatternList patternList = new INPatternList();
		patternList.add(new INIdentifierPattern(VAR));
		INMultipleBindList bindList = new INMultipleBindList();
		bindList.add(new INMultipleTypeBind(patternList, new TCBooleanType(LexLocation.ANY)));
		INExpression predicate = new INImpliesExpression(new INVariableExpression(VAR), IMP, UNDEFINED);
		INForAllExpression forall = new INForAllExpression(LexLocation.ANY, bindList, predicate);

		// forall x:bool & x => undefined
		assertTrue(forall.eval(ctxt).isUndefined());	// Because one case is undefined and none are false
	}

	public void testForall2() throws Exception
	{
		TCNameToken VAR = new TCNameToken(LexLocation.ANY, "DEFAULT", "x");
		INPatternList patternList = new INPatternList();
		patternList.add(new INIdentifierPattern(VAR));
		INExpressionList members = new INExpressionList();
		members.add(UNDEFINED);
		members.add(TRUE);
		members.add(FALSE);
		INSetEnumExpression set = new INSetEnumExpression(LexLocation.ANY, members);
		INMultipleBindList bindList = new INMultipleBindList();
		bindList.add(new INMultipleSetBind(patternList, set));
		INExpression predicate = new INVariableExpression(VAR);
		INForAllExpression forall = new INForAllExpression(LexLocation.ANY, bindList, predicate);

		// forall x in set {undefined, true, false} & x
		assertTrue(forall.eval(ctxt).equals(F));

		members.remove(FALSE);

		// forall x in set {undefined, true} & x
		assertTrue(forall.eval(ctxt).isUndefined());
	}

	public void testExists() throws Exception
	{
		TCNameToken VAR = new TCNameToken(LexLocation.ANY, "DEFAULT", "x");
		INPatternList patternList = new INPatternList();
		patternList.add(new INIdentifierPattern(VAR));
		INMultipleBindList bindList = new INMultipleBindList();
		bindList.add(new INMultipleTypeBind(patternList, new TCBooleanType(LexLocation.ANY)));
		INExpression predicate = new INImpliesExpression(new INVariableExpression(VAR), IMP, UNDEFINED);
		INExistsExpression exists = new INExistsExpression(LexLocation.ANY, bindList, predicate);

		// exists x:bool & x => undefined
		assertTrue(exists.eval(ctxt).equals(T));		// Because one case is true, regardless of undefined
	}

	public void testExists2() throws Exception
	{
		TCNameToken VAR = new TCNameToken(LexLocation.ANY, "DEFAULT", "x");
		INPatternList patternList = new INPatternList();
		patternList.add(new INIdentifierPattern(VAR));
		INExpressionList members = new INExpressionList();
		members.add(UNDEFINED);
		members.add(TRUE);
		members.add(FALSE);
		INSetEnumExpression set = new INSetEnumExpression(LexLocation.ANY, members);
		INMultipleBindList bindList = new INMultipleBindList();
		bindList.add(new INMultipleSetBind(patternList, set));
		INExpression predicate = new INVariableExpression(VAR);
		INExistsExpression forall = new INExistsExpression(LexLocation.ANY, bindList, predicate);

		// exists x in set {undefined, true, false} & x
		assertTrue(forall.eval(ctxt).equals(T));

		members.remove(TRUE);

		// exists x in set {undefined, false} & x
		assertTrue(forall.eval(ctxt).isUndefined());
	}
}