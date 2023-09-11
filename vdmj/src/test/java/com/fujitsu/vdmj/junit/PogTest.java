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
import com.fujitsu.vdmj.tc.TCRecursiveLoops;
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
		"(forall a:seq of (nat1) &\n  is_(inv_T2(a), bool))\n",
		"exists a : seq of (nat1) & (a <> [])\n",
		"forall m1, m2 in set {{1 |-> 2}, {2 |-> 3}} & forall d3 in set dom m1, d4 in set dom m2 & d3 = d4 => m1(d3) = m2(d4)\n",
		"After instance variable initializers (iv < 10)\n",
		"forall arg1:(int * int), arg2:seq of (int) & (exists bind1:(int * int), i:int, j:int & (arg1 = bind1) and (mk_(i, j) = bind1)) and (exists bind1:seq of (int), k:int & (arg2 = bind1) and ([k] = bind1))\n",
		"(forall mk_(i, j):(int * int), [k]:seq of (int) &\n  i in set dom m)\n",
		"(forall mk_(i, j):(int * int) &\n  i in set dom m)\n",
		"(let x:nat1 = 123 in\n  -1 in set dom m)\n",
		"(let x:nat1 = 123 in\n  ((m(-1) > 0) =>\n    1 in set dom m))\n",
		"(let x:nat1 = 123 in\n  (not (m(-1) > 0) =>\n    -2 in set dom m))\n",
		"(let x:nat1 = 123 in\n  (not (m(-1) > 0) =>\n    ((m(-2) > 0) =>\n      2 in set dom m)))\n",
		"(let x:nat1 = 123 in\n  (not (m(-1) > 0) =>\n    (not (m(-2) > 0) =>\n      (((x < 0) or ((x > 10) or (x = 100))) =>\n        3 in set dom m))))\n",
		"(let x:nat1 = 123 in\n  (not (m(-1) > 0) =>\n    (not (m(-2) > 0) =>\n      (not ((x < 0) or ((x > 10) or (x = 100))) =>\n        999 in set dom m))))\n",
		"(forall a:int, b:int & pre_pref(a, b) =>\n  a in set dom m)\n",
		"(forall a:int, b:int &\n  pre_prepostf(a, b) => post_prepostf(a, b, (a + b)))\n",
		"(forall mk_(a, b):(int * int), c:(int * int) &\n  is_(pre_prepostfi(mk_(a, b), c), bool))\n",
		"(forall mk_(a, b):(int * int), c:(int * int) &\n  pre_prepostfi(mk_(a, b), c) => exists r:int & post_prepostfi(mk_(a, b), c, r))\n",
		"(forall x:seq of (int) &\n  (exists [a]:seq1 of (int) & [a] = (x ^ [-999]) => let [a] = (x ^ [-999]) in\n    a in set dom m))\n",
		"(forall x:seq of (int) &\n  (not exists [a]:seq1 of (int) & [a] = (x ^ [-999]) =>\n    (exists [a, b]:seq1 of (int) & [a, b] = (x ^ [-999]) => let [a, b] = (x ^ [-999]) in\n      (a + b) in set dom m)))\n",
		"(forall x:seq of (int) &\n  (not exists [a]:seq1 of (int) & [a] = (x ^ [-999]) =>\n    (not exists [a, b]:seq1 of (int) & [a, b] = (x ^ [-999]) =>\n      (exists [a] ^ [b]:seq1 of (int) & ([a] ^ [b]) = (x ^ [-999]) => let [a] ^ [b] = (x ^ [-999]) in\n        (a + b) in set dom m))))\n",
		"(forall x:seq of (int) &\n  (not exists [a]:seq1 of (int) & [a] = (x ^ [-999]) =>\n    (not exists [a, b]:seq1 of (int) & [a, b] = (x ^ [-999]) =>\n      (not exists [a] ^ [b]:seq1 of (int) & ([a] ^ [b]) = (x ^ [-999]) =>\n        m(999) in set dom m))))\n",
		"(forall x:seq of (int) &\n  (not exists [a]:seq1 of (int) & [a] = (x ^ [-999]) =>\n    (not exists [a, b]:seq1 of (int) & [a, b] = (x ^ [-999]) =>\n      (not exists [a] ^ [b]:seq1 of (int) & ([a] ^ [b]) = (x ^ [-999]) =>\n        999 in set dom m))))\n",
		"(forall mk_(i, $any1):(int * int) &\n  exists x in set {m(1), 2, 3} & (m(x) < i))\n",
		"(forall mk_(i, $any1):(int * int) &\n  1 in set dom m)\n",
		"(forall mk_(i, $any1):(int * int) &\n  (forall x in set {m(1), 2, 3} &\n    x in set dom m))\n",
		"(forall mk_(i, $any1):(int * int) &\n  (forall x in set {m(1), 2, 3} & (m(x) < i) =>\n    x in set dom m))\n",
		"(let x:int = m(1) in\n  1 in set dom m)\n",
		"(let x:int = m(1) in\n  x in set dom m)\n",
		"(let local: int -> int local(x) == m(x) in\n  (forall x:int &\n    x in set dom m))\n",
		"1 in set dom m\n",
		"(let x = m(1) in\n  2 in set dom m)\n",
		"(def x = m(1); y = m(2) in\n  x in set dom m)\n",
		"(def x = m(1); y = m(2) in\n  y in set dom m)\n",
		"exists1 x in set {1, 2, 3} & (x < 10)\n",
		"(forall n:nat &\n  n > 1 => forall arg:nat & pre_f1(arg) => pre_f1(f1(arg)))\n",
		"(forall x:(int -> int), n:nat &\n  n > 1 => forall arg:nat & pre_(x, arg) => pre_(x, x(arg)))\n",
		"(forall n:nat &\n  n = 0 or n = 1 or rng(m) subset dom(m))\n",
		"(forall x:map (int) to (int), n:nat &\n  n = 0 or n = 1 or rng(x) subset dom(x))\n",
		"forall arg:int & pre_f2(arg) => pre_f1(f2(arg))\n",
		"(forall x:(int -> int), y:(int -> int) &\n  forall arg:int & pre_(y, arg) => pre_(x, y(arg)))\n",
		"(forall x:(int -> int) &\n  forall arg:int & pre_f2(arg) => pre_(x, f2(arg)))\n",
		"(forall x:(int -> int) &\n  forall arg:int & pre_(x, arg) => pre_f1(x(arg)))\n",
		"rng(m) subset dom(m)\n",
		"(forall x:map (int) to (int), y:map (int) to (int) &\n  rng(y) subset dom(x))\n",
		"(forall x:map (int) to (int) &\n  rng(m) subset dom(x))\n",
		"(forall i:int &\n  pre_f1(i))\n",
		"(forall x:(int -> int) &\n  pre_(x, 123))\n",
		"(forall i:int &\n  i in set inds sq)\n",
		"(forall x:seq of (int) &\n  123 in set inds x)\n",
		"(forall s:set of (set of (int)) &\n  s <> {})\n",
		"(forall s:seq of (int) &\n  (not (s = []) =>\n    s <> []))\n",
		"(forall i:int &\n  i <> 0)\n",
		"(forall i:int &\n  is_int((123 / i)))\n",
		"(forall i:int &\n  i <> 0)\n",
		"exists finmap1:map nat to (map (int) to (nat1)) & forall a:int, b in set {1, 2, 3} & (a < 10) => exists findex2 in set dom finmap1 & finmap1(findex2) = {a |-> b}\n",
		"exists finmap1:map nat to (int) & forall a:int, b in set {1, 2, 3} & (a < 10) => exists findex2 in set dom finmap1 & finmap1(findex2) = (a + b)\n",
		"(forall a:map (int) to (int), b:map (int) to (int) &\n  forall ldom1 in set dom a, rdom2 in set dom b & ldom1 = rdom2 => a(ldom1) = b(rdom2))\n",
		"(forall x:int &\n  forall m1, m2 in set {{1 |-> 2}, {2 |-> 3}, {x |-> 4}} & forall d3 in set dom m1, d4 in set dom m2 & d3 = d4 => m1(d3) = m2(d4))\n",
		"forall m1 in set {{1 |-> 2}, {2 |-> 3}}, m2 in set {{1 |-> 2}, {2 |-> 3}} & forall d3 in set dom m1, d4 in set dom m2 & d3 = d4 => m1(d3) = m2(d4)\n",
		"(forall n:nat &\n  is_(measure_recursive(n), nat))\n",
		"(forall n:nat &\n  (not (n = 1) =>\n    (n - 1) >= 0))\n",
		"(forall n:nat &\n  (not (n = 1) =>\n    measure_recursive(n) > measure_recursive((n - 1))))\n",
		"dom {1 |-> false} subset inds [2, true, 7.8]\n",
		"is_(([2, true, 7.8] ++ {1 |-> false}), seq of ((bool | nat)))\n",
		"(forall t:((nat * nat * nat) | (nat * nat)) &\n  is_(t, (nat * nat * nat)))\n",
		"(forall u:U &\n  (let mk_(a, b):U = u in\n    exists mk_(a, b):U & mk_(a, b) = u))\n",
		"(forall u:U &\n  (let mk_(a, b):U = u in\n    is_(u, (nat * nat))))\n",
		"(is_([1, 2, true], T1) and ((is_(1, bool)) and (is_(2, bool)))) or (is_([1, 2, true], T2) and inv_T2([1, 2, true]) and ((is_nat1(true))))\n",
		"(forall t:(T1 | T2) &\n  (let x:(T1 | T2) = [1, 2, 3, true] in\n    (is_([1, 2, 3, true], T1) and ((is_(1, bool)) and (is_(2, bool)) and (is_(3, bool)))) or (is_([1, 2, 3, true], T2) and inv_T2([1, 2, 3, true]) and ((is_nat1(true))))))\n",
		"(forall a:(T1 | T2 | int) &\n  (is_(a, T1)) or (is_(a, T2) and inv_T2(a) and (is_(a, seq of (nat1)))))\n",
		"is_({1 |-> \"2\"}, inmap nat1 to seq1 of (char))\n",
		"(forall n:nat1, x:nat1 &\n  (not (n < 2) =>\n    (n - 1) > 0))\n",
		"(forall n:nat1, x:nat1 &\n  (not (n < 2) =>\n    (x - 1) > 0))\n",
		"(forall n:nat1, x:nat1 &\n  (not (n < 2) =>\n    id(n, x) > id((n - 1), (x - 1))))\n",
		"(forall x:nat1, y:nat1 &\n  (x - y) >= 0)\n",
		"(forall mk_(n, x):(nat1 * nat1) &\n  (not (n < 2) =>\n    ((n - 1) > 0) and ((x - 1) > 0)))\n",
		"(forall mk_(n, x):(nat1 * nat1) &\n  (not (n < 2) =>\n    id2(mk_(n, x)) > id2(mk_((n - 1), (x - 1)))))\n",
		"(forall mk_(x, y):(nat1 * nat1) &\n  (x - y) >= 0)\n",
		"(forall mk_(n, x):(nat1 * nat1) &\n  (not (n < 2) =>\n    ((n - 1) > 0) and ((x - 1) > 0)))\n",
		"(forall mk_(n, x):(nat1 * nat1) &\n  (not (n < 2) =>\n    (let lhs = id3(mk_(n, x)), rhs = id3(mk_((n - 1), (x - 1))) in if lhs.#1 <> rhs.#1 then lhs.#1 > rhs.#1 else lhs.#2 > rhs.#2)))\n",
		"1 in set dom m\n",
		"1 in set dom m\n",
		"2 in set dom m\n",
		"3 in set dom m\n",
		"while (x > 0) do ...\n",
		"After iv := (iv + 1) (iv < 10)\n"
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
		ClassMapper.getInstance(PONode.MAPPINGS).convert(TCRecursiveLoops.getInstance());
		ProofObligationList polist = poglist.getProofObligations();

		// Copy this output to re-generate the expected from the actuals...
		int i = 0;

		for (ProofObligation po: polist)
		{
			Console.out.println(++i + " \"" + po.value.replaceAll("\n", "\\\\n") + "\",");
			assertTrue("PO type checked failed", !po.isCheckable || po.getCheckedExpression() != null);
		}

		i = 0;

		for (ProofObligation po: polist)
		{
			if (!expected[i].equals(po.value))
			{
				Console.out.println("PO# " + (i+1));
				Console.out.println("Expected: " + expected[i]);
				Console.out.println("Actual: " + po.value);
			}

			assertEquals("PO #" + (i+1), expected[i], po.value);
			i++;
		}

		assertEquals("POs generated", expected.length, polist.size());
	}
}
