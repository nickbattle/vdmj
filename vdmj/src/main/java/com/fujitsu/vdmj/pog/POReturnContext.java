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

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.tc.types.TCType;

public class POReturnContext extends POContext
{
	public final POPattern pattern;
	public final TCType type;
	public final POExpression result;
	
	public POReturnContext(POPattern pattern, TCType type, POExpression result)
	{
		this.pattern = pattern;
		this.type = type;
		this.result = result;
	}
	
	public POReturnContext()
	{
		this.pattern = null;
		this.type = null;
		this.result = null;
	}

	@Override
	public boolean returnsEarly()
	{
		return true;
	}

	@Override
	public String getSource()
	{
		if (result != null)
		{
			return "let " + pattern + " : " + type + " = " + result + " in";
		}
		else
		{
			return "";
		}
	}
}
