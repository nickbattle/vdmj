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

package com.fujitsu.vdmj.in.patterns;

/**
 * The base type for all INMultipleBind visitors. All methods, by default, call
 * the abstract caseMultipleBind method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class INMultipleBindVisitor<R, S>
{
 	abstract public R caseMultipleBind(INMultipleBind node, S arg);

 	public R caseMultipleSeqBind(INMultipleSeqBind node, S arg)
	{
		return caseMultipleBind(node, arg);
	}

 	public R caseMultipleSetBind(INMultipleSetBind node, S arg)
	{
		return caseMultipleBind(node, arg);
	}

 	public R caseMultipleTypeBind(INMultipleTypeBind node, S arg)
	{
		return caseMultipleBind(node, arg);
	}
}
