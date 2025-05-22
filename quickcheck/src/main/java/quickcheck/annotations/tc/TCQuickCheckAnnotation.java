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

package quickcheck.annotations.tc;

import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCNewExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class TCQuickCheckAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;
	
	public final TCParameterType qcParam;
	public final TCTypeList qcTypes;
	public final TCNewExpression qcConstructor;

	public TCQuickCheckAnnotation(TCIdentifierToken name,
		TCParameterType qcParam, TCTypeList qcTypes, TCNewExpression qcConstructor)
	{
		super(name, null);
		this.qcParam = qcParam;
		this.qcTypes = qcTypes;
		this.qcConstructor = qcConstructor;
	}
	
	@Override
	public String toString()
	{
		if (qcConstructor != null)
		{
			return "@" + name + " " + qcConstructor + ";";
		}
		else
		{
			return "@" + name + " " + qcParam + " = " + Utils.listToString("", qcTypes, ", ", ";");
		}
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		if (qcConstructor == null)
		{
			checkTypeParams(def, env, scope);
		}
		else
		{
			checkConstructor(def, env, scope);
		}
	}
	
	private void checkConstructor(TCDefinition def, Environment env, NameScope scope)
	{
		qcConstructor.typeCheck(env, null, scope, null);
	}

	private void checkTypeParams(TCDefinition def, Environment env, NameScope scope)
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
			name.report(6001, "@QuickCheck only applies to function definitions");
			return;
		}
		
		if (funcParams == null)
		{
			name.report(6001, "@QuickCheck only applies to polymorphic definitions");
		}
		else
		{
			for (TCType fParam: funcParams)
			{
				if (fParam.equals(qcParam))
				{
					TCParameterType tcp = (TCParameterType)fParam;
					
					if (tcp.paramPattern != null)	// @TypeParam available
					{
						for (TCType qcType: qcTypes)
						{
							if (!TypeComparator.compatible(tcp.paramPattern, qcType))
							{
								qcType.report(6001, "Inappropriate type for @QC parameter");
								qcType.detail2("Expect", tcp.paramPattern, "Actual", qcType);
							}
						}
					}
					
					return;		// Valid parameter name
				}
			}
			
			name.report(6001, "@QuickCheck " +  qcParam + " is not a parameter of " + def.name);
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
