/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package com.fujitsu.vdmj.po.annotations;

import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.RecursiveObligation;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCNaturalType;

public class POOperationMeasureAnnotation extends POAnnotation
{
	private static final long serialVersionUID = 1L;

	private final static String NAME = "MEASURE_";
	private final TCNameToken measureName;
	private final POExpression expression;
	
	public POOperationMeasureAnnotation(TCIdentifierToken name, POExpressionList args)
	{
		super(name, args);
		measureName = new TCNameToken(location, location.module, NAME + location.startLine);
		expression = args.get(0);
	}

	public POAssignmentDefinition getDefinition()	// let MEASURE_? = <expression> in...
	{
		TCNaturalType mtype = new TCNaturalType(location);
		return new POAssignmentDefinition(measureName, mtype, expression, mtype);
	}

	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		return RecursiveObligation.getAllPOs(location, expression, measureName, ctxt);
	}
}
