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
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTypeParamAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;
	
	public final TCParameterType qcParam;
	public final TCType qcType;

	public TCTypeParamAnnotation(TCIdentifierToken name, TCParameterType qcParam, TCType qcType)
	{
		super(name, null);
		this.qcParam = qcParam;
		this.qcType = qcType;
	}
	
	@Override
	public String toString()
	{
		return "@" + name + " " + qcParam + " = " + qcType;
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		TCTypeList funcParams = null;
		
		if (def instanceof TCExplicitFunctionDefinition)
		{
			TCExplicitFunctionDefinition exdef = (TCExplicitFunctionDefinition)def;
			funcParams = exdef.typeParams;
		}
		else if (def instanceof TCImplicitFunctionDefinition)
		{
			TCImplicitFunctionDefinition imdef = (TCImplicitFunctionDefinition)def;
			funcParams = imdef.typeParams;
		}
		else
		{
			name.report(6001, "@TypeParam only applies to function definitions");
			return;
		}
		
		if (funcParams == null)
		{
			name.report(6001, "@TypeParam only applies to polymorphic definitions");
		}
		else
		{
			for (TCType ptype: funcParams)
			{
				if (ptype.equals(qcParam))
				{
					TCParameterType paramType = (TCParameterType) ptype;
					paramType.paramPattern = qcType;
					return;		// Valid parameter name
				}
			}
			
			name.report(6001, "@TypeParam " +  qcParam + " is not a parameter of " + def.name);
		}
	}
	
	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		name.report(6001, "@TypeParam only applies to function definitions");
	}
	
	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		name.report(6001, "@TypeParam only applies to function definitions");
	}

	@Override
	public void tcBefore(TCModule m)
	{
		name.report(6001, "@TypeParam only applies to function definitions");
	}

	@Override
	public void tcBefore(TCClassDefinition m)
	{
		name.report(6001, "@TypeParam only applies to function definitions");
	}
}
