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

package com.fujitsu.vdmj.junit.overture.typecheck;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.junit.overture.OvertureTest;

public class ClassTest extends OvertureTest
{
	public void test_Typecheck1()
	{
		typecheck("typecheck1");
	}

	public void test_Typecheck2()
	{
		typecheck("typecheck2");
	}

	public void test_ReturnNil()
	{
		typecheck("returnnil");
	}

	public void test_VarAccess()
	{
		typecheck("varaccess");
	}

	public void test_Pure()
	{
		Settings.release = Release.VDM_10;
		typecheck("puretest");
	}

	public void test_Set1()
	{
		typecheck("set1");
	}
	
	public void test_SeqBinds()
	{
		Settings.release = Release.VDM_10;
		typecheck("seqbinds");
	}

	public void test_Cyclic()
	{
		typecheck("cyclic");
	}
}
