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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.junit;

import java.io.File;
import java.net.URL;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.definitions.POClassList;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import junit.framework.TestCase;

public class PogTest extends TestCase
{
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		Settings.release = Release.DEFAULT;
		Settings.dialect = Dialect.VDM_SL;
	}

	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	private String[] expected =
	{
			/* 1 */ "(forall a:T2! &\n  is_(inv_T2(a), bool))\n",
			/* 2 */ "exists a : seq of nat1 & (a <> [])\n",
			/* 3 */ "forall m1, m2 in set {{1 |-> 2}, {2 |-> 3}} & forall d3 in set dom m1, d4 in set dom m2 & d3 = d4 => m1(d3) = m2(d4)\n",
			/* 4 */ "After instance variable initializers (iv < 10)\n",
			/* 5 */ "forall arg1:(int) * (int), arg2:seq of int & (exists bind1:(int) * (int), i:int, j:int & (arg1 = bind1) and (mk_(i, j) = bind1)) and (exists bind1:seq of int, k:int & (arg2 = bind1) and ([k] = bind1))\n",
			/* 6 */ "(forall mk_(i, j):(int) * (int), [k]:seq of int &\n  i in set dom m)\n",
			/* 7 */ "(forall mk_(i, j):(int) * (int) &\n  i in set dom m)\n",
			/* 8 */ "(let x : nat1 = 123 in\n  -1 in set dom m)\n",
			/* 9 */ "(let x : nat1 = 123 in\n  ((m(-1) > 0) =>\n    1 in set dom m))\n",
			/* 10 */ "(let x : nat1 = 123 in\n  (not (m(-1) > 0) =>\n    -2 in set dom m))\n",
			/* 11 */ "(let x : nat1 = 123 in\n  (not (m(-1) > 0) =>\n    ((m(-2) > 0) =>\n      2 in set dom m)))\n",
			/* 12 */ "(let x : nat1 = 123 in\n  (not (m(-1) > 0) =>\n    (not (m(-2) > 0) =>\n      (((x < 0) or ((x > 10) or (x = 100))) =>\n        3 in set dom m))))\n",
			/* 13 */ "(let x : nat1 = 123 in\n  (not (m(-1) > 0) =>\n    (not (m(-2) > 0) =>\n      (not ((x < 0) or ((x > 10) or (x = 100))) =>\n        999 in set dom m))))\n",
			/* 14 */ "(forall a:int, b:int &\n  is_(pre_pref(a, b), bool))\n",
			/* 15 */ "(forall a:int, b:int & pre_pref(a, b) =>\n  a in set dom m)\n",
			/* 16 */ "(forall a:int, b:int &\n  is_(pre_prepostf(a, b), bool))\n",
			/* 17 */ "(forall a:int, b:int &\n  pre_prepostf(a, b) => post_prepostf(a, b, (a + b)))\n",
			/* 18 */ "(forall a:int, b:int, RESULT:int &\n  is_(post_prepostf(a, b, RESULT), bool))\n",
			/* 19 */ "(forall mk_(a, b):(int) * (int), c:(int) * (int) &\n  is_(pre_prepostfi(mk_(a, b), c), bool))\n",
			/* 20 */ "(forall mk_(a, b):(int) * (int), c:(int) * (int), r:int &\n  is_(post_prepostfi(mk_(a, b), c, r), bool))\n",
			/* 21 */ "(forall mk_(a, b):(int) * (int), c:(int) * (int) &\n  pre_prepostfi(mk_(a, b), c) => exists r:int & post_prepostfi(mk_(a, b), c, r))\n",
			/* 22 */ "(forall x:seq of int &\n  (exists [a]:seq1 of int & [a] = (x ^ [-999]) => let [a] = (x ^ [-999]) in\n    a in set dom m))\n",
			/* 23 */ "(forall x:seq of int &\n  (not (exists [a]:seq1 of int & [a] = (x ^ [-999])) =>\n    (exists [a, b]:seq1 of int & [a, b] = (x ^ [-999]) => let [a, b] = (x ^ [-999]) in\n      (a + b) in set dom m)))\n",
			/* 24 */ "(forall x:seq of int &\n  (not (exists [a]:seq1 of int & [a] = (x ^ [-999])) =>\n    (not (exists [a, b]:seq1 of int & [a, b] = (x ^ [-999])) =>\n      (exists [a] ^ [b]:seq1 of int & [a] ^ [b] = (x ^ [-999]) => let [a] ^ [b] = (x ^ [-999]) in\n        (a + b) in set dom m))))\n",
			/* 25 */ "(forall x:seq of int &\n  (not (exists [a]:seq1 of int & [a] = (x ^ [-999])) =>\n    (not (exists [a, b]:seq1 of int & [a, b] = (x ^ [-999])) =>\n      (not (exists [a] ^ [b]:seq1 of int & [a] ^ [b] = (x ^ [-999])) =>\n        m(999) in set dom m))))\n",
			/* 26 */ "(forall x:seq of int &\n  (not (exists [a]:seq1 of int & [a] = (x ^ [-999])) =>\n    (not (exists [a, b]:seq1 of int & [a, b] = (x ^ [-999])) =>\n      (not (exists [a] ^ [b]:seq1 of int & [a] ^ [b] = (x ^ [-999])) =>\n        999 in set dom m))))\n",
			/* 27 */ "(forall mk_(i, $any1):(int) * (int) &\n  exists x in set {m(1), 2, 3} & (m(x) < i))\n",
			/* 28 */ "(forall mk_(i, $any1):(int) * (int) &\n  1 in set dom m)\n",
			/* 29 */ "(forall mk_(i, $any1):(int) * (int) &\n  (forall x in set {m(1), 2, 3} &\n    x in set dom m))\n",
			/* 30 */ "(forall mk_(i, $any1):(int) * (int) &\n  (forall x in set {m(1), 2, 3} & (m(x) < i) =>\n    x in set dom m))\n",
			/* 31 */ "1 in set dom m\n",
			/* 32 */ "(let x : int = m(1) in\n  x in set dom m)\n",
			/* 33 */ "(let local: int -> int local(x) == m(x) in\n  (forall x:int &\n    x in set dom m))\n",
			/* 34 */ "1 in set dom m\n",
			/* 35 */ "(let x = m(1) in\n  2 in set dom m)\n",
			/* 36 */ "(let x = m(1), y = m(2) in\n  x in set dom m)\n",
			/* 37 */ "(let x = m(1), y = m(2) in\n  y in set dom m)\n",
			/* 38 */ "exists1 x in set {1, 2, 3} & (x < 10)\n",
			/* 39 */ "(forall i:int &\n  is_(pre_f1(i), bool))\n",
			/* 40 */ "(forall i:int &\n  is_(pre_f2(i), bool))\n",
			/* 41 */ "(forall n:nat &\n  n > 1 => forall arg:nat & pre_f1(arg) => pre_f1(f1(arg)))\n",
			/* 42 */ "(forall x:int -> int, n:nat &\n  n > 1 => forall arg:nat & pre_(x, arg) => pre_(x, x(arg)))\n",
			/* 43 */ "(forall n:nat &\n  n = 0 or n = 1 or rng(m) subset dom(m))\n",
			/* 44 */ "(forall x:map int to int, n:nat &\n  n = 0 or n = 1 or rng(x) subset dom(x))\n",
			/* 45 */ "forall arg:int & pre_f2(arg) => pre_f1(f2(arg))\n",
			/* 46 */ "(forall x:int -> int, y:int -> int &\n  forall arg:int & pre_(y, arg) => pre_(x, y(arg)))\n",
			/* 47 */ "(forall x:int -> int &\n  forall arg:int & pre_f2(arg) => pre_(x, f2(arg)))\n",
			/* 48 */ "(forall x:int -> int &\n  forall arg:int & pre_(x, arg) => pre_f1(x(arg)))\n",
			/* 49 */ "rng(m) subset dom(m)\n",
			/* 50 */ "(forall x:map int to int, y:map int to int &\n  rng(y) subset dom(x))\n",
			/* 51 */ "(forall x:map int to int &\n  rng(m) subset dom(x))\n",
			/* 52 */ "(forall i:int &\n  pre_f1(i))\n",
			/* 53 */ "(forall i:int &\n  i in set inds sq)\n",
			/* 54 */ "(forall x:seq of int &\n  123 in set inds x)\n",
			/* 55 */ "(forall s:set of (set of int) &\n  s <> {})\n",
			/* 56 */ "(forall s:seq of int &\n  (not (s = []) =>\n    s <> []))\n",
			/* 57 */ "(forall i:int &\n  i <> 0)\n",
			/* 58 */ "(forall i:int &\n  is_int((123 / i)))\n",
			/* 59 */ "(forall i:int &\n  i <> 0)\n",
			/* 60 */ "exists finmap1:map nat to (map int to nat1) & forall a:int, b in set {1, 2, 3} & (a < 10) => exists findex2 in set dom finmap1 & finmap1(findex2) = {a |-> b}\n",
			/* 61 */ "exists finmap1:map nat to (int) & forall a:int, b in set {1, 2, 3} & (a < 10) => exists findex2 in set dom finmap1 & finmap1(findex2) = (a + b)\n",
			/* 62 */ "(forall a:map int to int, b:map int to int &\n  forall ldom1 in set dom a, rdom2 in set dom b & ldom1 = rdom2 => a(ldom1) = b(rdom2))\n",
			/* 63 */ "(forall x:int &\n  forall m1, m2 in set {{1 |-> 2}, {2 |-> 3}, {x |-> 4}} & forall d3 in set dom m1, d4 in set dom m2 & d3 = d4 => m1(d3) = m2(d4))\n",
			/* 64 */ "forall m1 in set {{1 |-> 2}, {2 |-> 3}}, m2 in set {{1 |-> 2}, {2 |-> 3}} & forall d3 in set dom m1, d4 in set dom m2 & d3 = d4 => m1(d3) = m2(d4)\n",
			/* 65 */ "(forall n:nat &\n  is_(measure_recursive(n), nat))\n",
			/* 66 */ "(forall n:nat &\n  (not (n = 1) =>\n    (n - 1) >= 0))\n",
			/* 67 */ "(forall n:nat &\n  (not (n = 1) =>\n    measure_recursive(n) > measure_recursive(n - 1)))\n",
			/* 68 */ "dom {1 |-> false} subset inds [2, true, 7.8]\n",
			/* 69 */ "is_(([2, true, 7.8] ++ {1 |-> false}), seq of (bool | nat))\n",
			/* 70 */ "(forall t:((nat) * (nat) * (nat)) | ((nat) * (nat)) &\n  is_(t, (nat) * (nat) * (nat)))\n",
			/* 71 */ "(forall u:U &\n  exists mk_(a, b):U & mk_(a, b) = u)\n",
			/* 72 */ "(forall u:U &\n  is_(u, (nat) * (nat)))\n",
			/* 73 */ "(is_([1, 2, true], T1) and ((is_(1, bool)) and (is_(2, bool)))) or (is_([1, 2, true], T2) and inv_T2([1, 2, true]))\n",
			/* 74 */ "(forall t:T1 | T2 &\n  (is_([1, 2, 3, true], T1) and ((is_(1, bool)) and (is_(2, bool)) and (is_(3, bool)))) or (is_([1, 2, 3, true], T2) and inv_T2([1, 2, 3, true])))\n",
			/* 75 */ "(forall a:T1 | T2 | int &\n  (is_(a, T1) and (is_(a, seq of bool))) or (is_(a, T2) and inv_T2(a)))\n",
			/* 76 */ "is_({1 |-> \"2\"}, inmap nat1 to seq1 of char)\n",
			/* 77 */ "(forall n:nat1, x:nat1 &\n  (not (n < 2) =>\n    (n - 1) > 0))\n",
			/* 78 */ "(forall n:nat1, x:nat1 &\n  (not (n < 2) =>\n    (x - 1) > 0))\n",
			/* 79 */ "(forall n:nat1, x:nat1 &\n  (not (n < 2) =>\n    id(n, x) > id(n - 1, x - 1)))\n",
			/* 80 */ "(forall x:nat1, y:nat1 &\n  (x - y) >= 0)\n",
			/* 81 */ "(forall mk_(n, x):(nat1) * (nat1) &\n  (not (n < 2) =>\n    ((n - 1) > 0) and ((x - 1) > 0)))\n",
			/* 82 */ "(forall mk_(n, x):(nat1) * (nat1) &\n  (not (n < 2) =>\n    id2(mk_(n, x)) > id2(mk_((n - 1), (x - 1)))))\n",
			/* 83 */ "(forall mk_(x, y):(nat1) * (nat1) &\n  (x - y) >= 0)\n",
			/* 84 */ "(forall mk_(n, x):(nat1) * (nat1) &\n  (not (n < 2) =>\n    ((n - 1) > 0) and ((x - 1) > 0)))\n",
			/* 85 */ "(forall mk_(n, x):(nat1) * (nat1) &\n  (not (n < 2) =>\n    (let lhs = id3(mk_(n, x)), rhs = id3(mk_((n - 1), (x - 1))) in if lhs.#1 <> rhs.#1 then lhs.#1 > rhs.#1 else lhs.#2 > rhs.#2)))\n",
			/* 86 */ "(forall i:int, obj_A(iv |-> iv):A &\n  (1 = i => \n    1 in set dom m))\n",
			/* 87 */ "(forall i:int, obj_A(iv |-> iv):A &\n  (1 = i => \n    ((m(1) < 10) =>\n      1 in set dom m)))\n",
			/* 88 */ "(forall i:int, obj_A(iv |-> iv):A &\n  (1 = i => \n    (not (m(1) < 10) =>\n      2 in set dom m)))\n",
			/* 89 */ "(forall i:int, obj_A(iv |-> iv):A &\n  (not 1 = i =>\n    (2 = i => \n      3 in set dom m)))\n",
			/* 90 */ "(forall obj_A(iv |-> iv):A &\n  (let x : int = 10 in\n    -- Missing loop invariant))\n",
			/* 91 */ "(forall obj_A(iv |-> iv):A &\n  (let x : int = 10 in\n    (let iv : int = (iv + 1) in\n      (iv < 10))))\n",
	};

	public void testPOG() throws Exception
	{
		URL rurl = getClass().getResource("/pogtest/pog.vpp");
		String file = rurl.getPath();

		LexTokenReader ltr = new LexTokenReader(new File(file), Dialect.VDM_PP);
		ClassReader cr = new ClassReader(ltr);
		ASTClassList parsed = cr.readClasses();

		TCClassList checked = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(parsed);
		TypeChecker typeChecker = new ClassTypeChecker(checked);
		typeChecker.typeCheck();
		TypeChecker.printErrors(Console.out);
		assertEquals("Spec type check errors", 0, TypeChecker.getErrorCount());

		POClassList poglist = ClassMapper.getInstance(PONode.MAPPINGS).init().convert(checked);
		ProofObligationList polist = poglist.getProofObligations();

		// Copy this output to re-generate the expected from the actuals...
		int i = 0;

		for (ProofObligation po: polist)
		{
			Console.out.println("/* " + ++i + " */ \"" + po.source.replaceAll("\n", "\\\\n") + "\",");
			assertTrue("PO type checked failed", !po.isCheckable || po.getCheckedExpression() != null);
		}

		i = 0;
		int errs = 0;

		for (ProofObligation po: polist)
		{
			if (!expected[i].equals(po.source))
			{
				Console.out.println("PO# " + (i+1));
				Console.out.print("Expected: " + expected[i]);
				Console.out.print("Actual: " + po.source);
				errs++;
			}

			i++;
		}

		assertEquals("POs failed", 0, errs);
		assertEquals("POs generated", expected.length, polist.size());
	}
}
