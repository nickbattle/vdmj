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

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

public class PORecordModifier extends PONode
{
	private static final long serialVersionUID = 1L;

	public final TCIdentifierToken tag;
	public final POExpression value;

	public PORecordModifier(TCIdentifierToken tag, POExpression value)
	{
		this.tag = tag;
		this.value = value;
	}

	@Override
	public String toString()
	{
		return tag + " |-> " + value;
	}
}
