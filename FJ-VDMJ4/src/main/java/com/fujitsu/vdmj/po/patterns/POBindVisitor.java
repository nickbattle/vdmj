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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.patterns;

/**
 * The base type for all ASTBind visitors. All methods, by default, call
 * the abstract caseBind method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class POBindVisitor<R, S>
{
 	abstract public R caseBind(POBind node, S arg);

 	public R caseSeqBind(POSeqBind node, S arg)
	{
		return caseBind(node, arg);
	}

 	public R caseSetBind(POSetBind node, S arg)
	{
		return caseBind(node, arg);
	}

 	public R caseTypeBind(POTypeBind node, S arg)
	{
		return caseBind(node, arg);
	}
}
