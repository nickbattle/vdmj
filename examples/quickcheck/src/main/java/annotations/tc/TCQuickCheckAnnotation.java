/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package annotations.tc;

import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCFuncInstantiationExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCQuickCheckAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;

	public TCQuickCheckAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		TCTypeList typeParams = null;
		boolean found = false;
		
		if (def instanceof TCExplicitFunctionDefinition)
		{
			TCExplicitFunctionDefinition exdef = (TCExplicitFunctionDefinition)def;
			typeParams = exdef.typeParams;
			found = true;
		}
		else if (def instanceof TCImplicitFunctionDefinition)
		{
			TCImplicitFunctionDefinition imdef = (TCImplicitFunctionDefinition)def;
			typeParams = imdef.typeParams;
			found = true;
		}
		
		if (!found)
		{
			name.report(6001, "@QuickCheck only applies to function definitions");
		}
		else if (typeParams == null)
		{
			name.report(6001, "@QuickCheck only applies to polymorphic definitions");
		}
		else
		{
			TCFuncInstantiationExpression exp = (TCFuncInstantiationExpression) args.get(0);
			String T = exp.actualTypes.get(0).toString();
			
			for (TCType ptype: typeParams)
			{
				if (ptype.toString().equals(T))
				{
					return;
				}
			}
			
			name.report(6001, "@QuickCheck " +  T + " is not a parameter");
		}
	}
	
	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		name.report(6001, "@QuickCheck only applies to function definitions");
	}
	
	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		name.report(6001, "@QuickCheck only applies to function definitions");
	}

	@Override
	public void tcBefore(TCModule m)
	{
		name.report(6001, "@QuickCheck only applies to function definitions");
	}

	@Override
	public void tcBefore(TCClassDefinition m)
	{
		name.report(6001, "@QuickCheck only applies to function definitions");
	}
}
