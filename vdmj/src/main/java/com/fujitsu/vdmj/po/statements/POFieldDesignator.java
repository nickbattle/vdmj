
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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POFieldExpression;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;

public class POFieldDesignator extends POStateDesignator
{
	private static final long serialVersionUID = 1L;
	public final POStateDesignator object;
	public final TCIdentifierToken field;
	public final TCRecordType recType;
	public final TCClassType clsType;

	public POFieldDesignator(POStateDesignator object,
			TCIdentifierToken field, TCRecordType recType, TCClassType clsType)
	{
		super(object.location);
		this.object = object;
		this.field = field;
		this.recType = recType;
		this.clsType = clsType;
	}
	
	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		return object.getProofObligations(ctxt);
	}
	
	@Override
	public POExpression toExpression()
	{
		POExpression root = object.toExpression();
		return new POFieldExpression(root, field, new TCNameToken(location, location.module, field.getName()));
	}

	@Override
	public String toString()
	{
		return object + "." + field;
	}

	/**
	 * The simple updated variable name, x := 1, x(i) := 1 and x(i)(2).fld := 1
	 * all return the updated variable "x".
	 */
	@Override
	public TCNameToken updatedVariableName()
	{
		return object.updatedVariableName();
	}

	/**
	 * The updated variable type, x := 1, x(i) := 1 and x(i)(2).fld := 1
	 * all return the type of the variable "x".
	 */
	@Override
	public TCType updatedVariableType()
	{
		return object.updatedVariableType();
	}
	
	/**
	 * All variables used in a designator, eg. m(x).fld(y) is {m, x, y}
	 */
	@Override
	public TCNameSet getVariableNames()
	{
		return object.getVariableNames();
	}
	
	/**
	 * All expressions used in a designator, eg. m(x).fld(y) is {m, x, y}
	 */
	@Override
	public POExpressionList getExpressions()
	{
		return object.getExpressions();
	}
}
