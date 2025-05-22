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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * The root of the state designator hierarchy.
 */
public abstract class POStateDesignator extends PONode
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;

	public POStateDesignator(LexLocation location)
	{
		this.location = location;
	}

	@Override
	abstract public String toString();
	
	abstract public POExpression toExpression();

	/**
	 * The simple updated variable name, x := 1, x(i) := 1 and x(i)(2).fld := 1
	 * all return the updated variable "x".
	 */
	abstract public TCNameToken updatedVariableName();

	/**
	 * The updated variable type, x := 1, x(i) := 1 and x(i)(2).fld := 1
	 * all return the type of the variable "x".
	 */
	abstract public TCType updatedVariableType();
	
	/**
	 * All variables used in a designator, eg. m(x).fld(y) is {m, x, y}
	 */
	abstract public TCNameSet getVariableNames();
	
	/**
	 * All expressions used in a designator, eg. m(x).fld(y) is {m, x, y}
	 */
	abstract public POExpressionList getExpressions();

	/**
	 * @param ctxt
	 */
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		return new ProofObligationList();
	}
}
