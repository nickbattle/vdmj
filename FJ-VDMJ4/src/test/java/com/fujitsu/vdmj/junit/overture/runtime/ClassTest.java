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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.junit.overture.runtime;

import com.fujitsu.vdmj.junit.overture.OvertureTest;

public class ClassTest extends OvertureTest
{
	public void test_Runtime1() throws Exception
	{
		runtime("runtime1");
	}

	public void test_Runtime2() throws Exception
	{
		runtime("runtime2");
	}

	public void test_Runtime3() throws Exception
	{
		runtime("runtime3");
	}

	public void test_Set1() throws Exception
	{
		runtime("set1");
	}
	
	public void test_SeqBinds() throws Exception
	{
		runtime("seqbinds");
	}
}
