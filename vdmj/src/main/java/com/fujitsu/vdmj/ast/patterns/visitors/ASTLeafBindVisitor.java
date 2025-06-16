/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package com.fujitsu.vdmj.ast.patterns.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.ast.ASTVisitorSet;
import com.fujitsu.vdmj.ast.patterns.ASTBind;
import com.fujitsu.vdmj.ast.patterns.ASTSeqBind;
import com.fujitsu.vdmj.ast.patterns.ASTSetBind;
import com.fujitsu.vdmj.ast.patterns.ASTTypeBind;

/**
 * This ASTBind visitor visits all of the leaves of a bind tree and calls
 * the basic processing methods for the simple cases.
 */
public abstract class ASTLeafBindVisitor<E, C extends Collection<E>, S> extends ASTBindVisitor<C, S>
{
	protected ASTVisitorSet<E, C, S> visitorSet = new ASTVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			bindVisitor = ASTLeafBindVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return ASTLeafBindVisitor.this.newCollection();
		}
	};

 	@Override
	abstract public C caseBind(ASTBind node, S arg);

 	@Override
	public C caseSeqBind(ASTSeqBind node, S arg)
	{
 		C all = visitorSet.applyPatternVisitor(node.pattern, arg);
 		all.addAll(visitorSet.applyExpressionVisitor(node.sequence, arg));
		return all;
	}

 	@Override
	public C caseSetBind(ASTSetBind node, S arg)
	{
 		C all = visitorSet.applyPatternVisitor(node.pattern, arg);
 		all.addAll(visitorSet.applyExpressionVisitor(node.set, arg));
		return all;
	}

 	@Override
	public C caseTypeBind(ASTTypeBind node, S arg)
	{
 		C all = visitorSet.applyPatternVisitor(node.pattern, arg);
 		all.addAll(visitorSet.applyTypeVisitor(node.type, arg));
		return all;
	}

 	abstract protected C newCollection();
}
