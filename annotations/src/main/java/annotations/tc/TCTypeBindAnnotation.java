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

package annotations.tc;

import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCTypeBindAnnotation extends TCAnnotation
{
	private final TCMultipleTypeBind typebind;
	private final TCExpression expression;

	public TCTypeBindAnnotation(TCIdentifierToken name, TCMultipleTypeBind typebind, TCExpression expression)
	{
		super(name, null);
		this.typebind = typebind;
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return "@" + name + " " + typebind + " = " + expression + ";";
	}
	
	@Override
	public void tcAfter(TCExpression exp, TCType type, Environment env, NameScope scope)
	{
		TCType mbtype = typebind.typeCheck(env, scope);
		TCType extype = expression.typeCheck(env, null, scope, null);

		if (TypeChecker.getErrorCount() > 0)
		{
			return;
		}

		TCSetType set = extype.getSet();

		if (set == null)
		{
			name.report(6001, expression + " is not a set");
			return;
		}

		if (!TypeComparator.isSubType(set.setof, mbtype))
		{
			name.report(6001, set.setof + " not subtype of " + mbtype);
			return;
		}

		if (exp instanceof TCForAllExpression)
		{
			TCForAllExpression forall = (TCForAllExpression)exp;

			if (!hasTypeBind(forall.bindList))
			{
				name.report(6001, "Forall expression does not have type bind " + typebind);
			}
		}
		else if (exp instanceof TCExistsExpression)
		{
			TCExistsExpression exists = (TCExistsExpression)exp;

			if (!hasTypeBind(exists.bindList))
			{
				name.report(6001, "Exists expression does not have type bind " + typebind);
			}
		}
		else
		{
			name.report(6001, "@TypeBind only applies to forall and exists expressions");
		}
	}

	private boolean hasTypeBind(TCMultipleBindList bindList)
	{
		for (TCMultipleBind mbind: bindList)
		{
			if (mbind instanceof TCMultipleTypeBind)
			{
				TCMultipleTypeBind mtbind = (TCMultipleTypeBind)mbind;

				if (mtbind.toString().equals(typebind.toString()))
				{
					return true;
				}
			}
		}

		return false;
	}

	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		name.report(6001, "@TypeBind only applies to forall and exists expressions");
	}
	
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		name.report(6001, "@TypeBind only applies to forall and exists expressions");
	}

	public void tcBefore(TCModule m)
	{
		name.report(6001, "@TypeBind only applies to forall and exists expressions");
	}

	public void tcBefore(TCClassDefinition clazz)
	{
		name.report(6001, "@TypeBind only applies to forall and exists expressions");
	}
}
