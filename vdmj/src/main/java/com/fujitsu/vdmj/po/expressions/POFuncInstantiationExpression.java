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

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.util.Utils;

public class POFuncInstantiationExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression function;
	public final TCTypeList actualTypes;
	public final TCFunctionType type;
	public final POExplicitFunctionDefinition expdef;
	public final POImplicitFunctionDefinition impdef;

	public POFuncInstantiationExpression(POExpression function,
		TCTypeList actualTypes,
		TCFunctionType type,
		POExplicitFunctionDefinition expdef,
		POImplicitFunctionDefinition impdef)
	{
		super(function);
		this.function = function;
		this.actualTypes = actualTypes;
		this.type = type;
		this.expdef = expdef;
		this.impdef = impdef;
	}

	@Override
	public String toString()
	{
		return "(" + function + ")[" + Utils.listToString(actualTypes) + "]";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		return function.getProofObligations(ctxt, env);
	}

	@Override
	public String getPreName()
	{
		return function.getPreName();
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseFuncInstantiationExpression(this, arg);
	}
}
