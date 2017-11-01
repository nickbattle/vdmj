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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCMultiBindListDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
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
	private TCDefinition def = null;

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
			paramDefinitions.addAll(tb.pattern.getDefinitions(tb.type, NameScope.LOCAL));
			paramPatterns.add(tb.pattern);
			ptypes.add(tb.type.typeResolve(base, null));
		}

		paramDefinitions.implicitDefinitions(base);
		paramDefinitions.typeCheck(base, scope);

		def = new TCMultiBindListDefinition(location, mbinds);
		def.typeCheck(base, scope);
		Environment local = new FlatCheckedEnvironment(def, base, scope);
		local.setEnclosingDefinition(def); 	// Prevent recursive checks
		local.setFunctional(true);
		TCType result = expression.typeCheck(local, null, scope, null);
		local.unusedCheck();

		type = new TCFunctionType(location, ptypes, true, result);
		return type;
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env)
	{
		TCNameSet names = new TCNameSet();	// Body expression is conditional
		
		for (TCTypeBind mb: bindList)
		{
			names.addAll(mb.getFreeVariables(globals, env));
		}
		
		return names;
	}
}
