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

package com.fujitsu.vdmj.ast.patterns.visitors;

import com.fujitsu.vdmj.ast.patterns.ASTMultipleBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleSeqBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleSetBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleTypeBind;

/**
 * The base type for all ASTMultipleBind visitors. All methods, by default, call
 * the abstract caseMultipleBind method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class ASTMultipleBindVisitor<R, S>
{
 	abstract public R caseMultipleBind(ASTMultipleBind node, S arg);

 	public R caseMultipleSeqBind(ASTMultipleSeqBind node, S arg)
	{
		return caseMultipleBind(node, arg);
	}

 	public R caseMultipleSetBind(ASTMultipleSetBind node, S arg)
	{
		return caseMultipleBind(node, arg);
	}

 	public R caseMultipleTypeBind(ASTMultipleTypeBind node, S arg)
	{
		return caseMultipleBind(node, arg);
	}
}
