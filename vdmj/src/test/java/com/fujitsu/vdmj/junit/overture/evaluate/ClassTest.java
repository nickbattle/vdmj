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

package com.fujitsu.vdmj.junit.overture.evaluate;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.junit.overture.OvertureTest;
import com.fujitsu.vdmj.lex.Dialect;

public class ClassTest extends OvertureTest
{
	public void test_Evaluate1()
	{
		evaluate("evaluate1", ResultType.TRUE);
	}

	public void test_Evaluate2()
	{
		evaluate("evaluate2", ResultType.VOID);
	}

	public void test_IotaTypeBind()
	{
		evaluate("iota_type_bind", ResultType.TRUE);
	}

	public void test_Patterns()
	{
		evaluate("patterns", ResultType.TRUE, 0, Release.VDM_10);
	}

	public void test_Measures()
	{
		evaluate("measures", ResultType.VOID);
	}

	public void test_SeqComp()
	{
		evaluate("seqcomp", ResultType.TRUE);
	}

	public void test_Postcond()
	{
		evaluate("postcond", ResultType.TRUE);
	}

	public void test_Typeparams()
	{
		evaluate("typeparams", ResultType.TRUE);
	}

	public void test_Setrange()
	{
		evaluate("setrange", ResultType.TRUE);
	}

	public void test_Inference()
	{
		evaluate("inference", ResultType.TRUE);
	}

	public void test_Narrow()
	{
		evaluate("narrow", ResultType.TRUE, 0, Release.VDM_10);
	}

	public void test_Curried()
	{
		evaluate("curried", ResultType.TRUE);
	}

	public void test_Stop()
	{
		Settings.dialect = Dialect.VDM_RT;
		evaluate("stoptest", ResultType.VOID, 0, Release.VDM_10);
	}

	public void test_Sporadic()
	{
		Settings.dialect = Dialect.VDM_RT;
		evaluate("sporadic", ResultType.VOID, 0, Release.VDM_10);
	}

	public void test_Set1()
	{
		evaluate("set1", ResultType.TRUE, 0, Release.VDM_10);
	}
	
	public void test_SeqBinds()
	{
		Settings.dialect = Dialect.VDM_RT;
		evaluate("seqbinds", ResultType.TRUE, 0, Release.VDM_10);
	}
	
	public void test_Ordering()
	{
		evaluate("ordering", ResultType.TRUE, 0, Release.VDM_10);
	}
	
	public void test_Ordering2()
	{
		evaluate("ordering2", ResultType.TRUE, 0, Release.VDM_10);
	}
}
