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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCMultiBindListDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBindList;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCLambdaExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCTypeBindList bindList;
	public final TCExpression expression;

	private TCFunctionType type;
	private TCPatternList paramPatterns;
	private TCDefinitionList paramDefinitions;
	public TCDefinition def = null;

	public TCLambdaExpression(LexLocation location, TCTypeBindList bindList, TCExpression expression)
	{
		super(location);
		this.bindList = bindList;
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return "(lambda " + bindList + " & " + expression + ")";
	}

	@Override
	public TCType typeCheck(Environment base, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		scope = NameScope.NAMES;	// lambdas are always functions, even if defined in operations
		TCMultipleBindList mbinds = new TCMultipleBindList();
		TCTypeList ptypes = new TCTypeList();

		paramPatterns = new TCPatternList();
		paramDefinitions = new TCDefinitionList();

		for (TCTypeBind tb: bindList)
		{
			mbinds.addAll(tb.getMultipleBindList());
			tb.typeResolve(base);
			paramDefinitions.addAll(tb.pattern.getDefinitions(tb.type, NameScope.LOCAL));
			paramPatterns.add(tb.pattern);
			ptypes.add(tb.type);
		}

		paramDefinitions.implicitDefinitions(base);
		paramDefinitions.typeCheck(base, scope);

		def = new TCMultiBindListDefinition(location, mbinds);
		def.typeCheck(base, scope);
		Environment local = new FlatCheckedEnvironment(def, base, scope);
		local.setEnclosingDefinition(def); 	// Prevent recursive checks
		local.setFunctional(true, true);
		TCType result = expression.typeCheck(local, null, scope, null);
		local.unusedCheck();

		type = new TCFunctionType(location, ptypes, true, result);
		return setType(type);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseLambdaExpression(this, arg);
	}
}
