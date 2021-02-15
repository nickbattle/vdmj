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

package com.fujitsu.vdmj.ast.types;

import java.util.Vector;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("serial")
public class ASTTypeList extends Vector<ASTType>
{
	public ASTTypeList()
	{
		super();
	}

	public ASTTypeList(ASTType act)
	{
		add(act);
	}

	@Override
	public boolean add(ASTType t)
	{
		return super.add(t);
	}

	@Override
	public String toString()
	{
		return "(" + Utils.listToString(this) + ")";
	}
}
