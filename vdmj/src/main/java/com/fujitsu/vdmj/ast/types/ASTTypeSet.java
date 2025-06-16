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

package com.fujitsu.vdmj.ast.types;

import java.util.TreeSet;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("serial")
public class ASTTypeSet extends TreeSet<ASTType>
{
	public ASTTypeSet()
	{
		super();
	}

	public ASTTypeSet(ASTType t)
	{
		add(t);
	}

	public ASTTypeSet(ASTType t1, ASTType t2)
	{
		add(t1);
		add(t2);
	}

	@Override
	public String toString()
	{
		return Utils.setToString(this, ", ");
	}
}
