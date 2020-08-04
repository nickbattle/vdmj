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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

public class INRecordModifier extends INNode
{
	private static final long serialVersionUID = 1L;

	public final TCIdentifierToken tag;
	public final INExpression value;

	public INRecordModifier(TCIdentifierToken tag, INExpression value)
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
