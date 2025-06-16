/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package annotations.tc;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCSystemDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCHistoryExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * Abstract root of all (validation) conjecture annotations. This class
 * provides various type check support methods that are common.
 */
abstract public class TCConjectureAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;

	public TCConjectureAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}
	
	@Override
	protected void doInit(Environment globals)
	{
		// this.globals = globals;
	}

	abstract protected void typeCheck(Environment env);

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		if (def instanceof TCExplicitOperationDefinition)
		{
			TCExplicitOperationDefinition op = (TCExplicitOperationDefinition)def;
			
			if (op.isConstructor && op.classDefinition instanceof TCSystemDefinition)
			{
				typeCheck(env);
			}
		}
		else if (def instanceof TCImplicitOperationDefinition)
		{
			TCImplicitOperationDefinition op = (TCImplicitOperationDefinition)def;
			
			if (op.isConstructor && op.classDefinition instanceof TCSystemDefinition)
			{
				typeCheck(env);
			}
		}
		else
		{
			name.report(6009, "@" + name + " only applies to system class constructors");
		}
	}

	@Override
	public void tcBefore(TCModule module)
	{
		name.report(6009, "@" + name + " only applies to system class constructors");
	}

	@Override
	public void tcBefore(TCClassDefinition clazz)
	{
		name.report(6009, "@" + name + " only applies to system class constructors");
	}

	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		name.report(6009, "@" + name + " only applies to system class constructors");
	}

	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		name.report(6009, "@" + name + " only applies to system class constructors");
	}

	protected boolean checkHistoryExpression(Environment env, TCExpression exp)
	{
		if (exp instanceof TCHistoryExpression)
		{
			TCHistoryExpression hexp = (TCHistoryExpression)exp;
			TCNameToken op = hexp.opnames.get(0);
			
			if (!op.isExplicit())
			{
				hexp.report(6008, "Name must be explicit '<name>`" + op + "'");
			}
			
			if (env.findMatches(op).isEmpty())
			{
				hexp.report(6008, "Name '" + op + "' is not in scope");
				return false;
			}
			
			return true;
		}
		else
		{
			exp.report(6008, "Expecting history expression (eg. #fin(op))");
			return false;
		}
	}
	
	protected boolean checkBooleanExpression(Environment env, TCExpression tcExpression)
	{
		return true;	// Can't check expressions yet...
		
//		TCType type = tcExpression.typeCheck(env, null, NameScope.ANYTHING, null);
//		
//		if (type.isType(TCBooleanType.class, LexLocation.ANY))
//		{
//			return true;
//		}
//		else
//		{
//			tcExpression.report(6008, "Expecting boolean expression, got " + type);
//			return false;
//		}
	}
	
	protected boolean checkNumericExpression(Environment env, TCExpression exp)
	{
		TCType type = exp.typeCheck(env, null, NameScope.ANYTHING, null);
		
		if (!type.isNumeric(LexLocation.ANY))
		{
			exp.report(6008, "Expecting numeric expression, got " + type);
			return false;
		}
		
		return true;
	}
}
