/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.tc.patterns.visitors;

import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;

/**
 * The base type for all TCBind visitors. All methods, by default, call
 * the abstract caseBind method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class TCBindVisitor<R, S>
{
 	abstract public R caseBind(TCBind node, S arg);

 	public R caseSeqBind(TCSeqBind node, S arg)
	{
		return caseBind(node, arg);
	}

 	public R caseSetBind(TCSetBind node, S arg)
	{
		return caseBind(node, arg);
	}

 	public R caseTypeBind(TCTypeBind node, S arg)
	{
		return caseBind(node, arg);
	}
}
