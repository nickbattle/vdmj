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

import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCInstantiate;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class TCFuncInstantiationExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression function;
	public final TCTypeList unresolved;
	public TCTypeList actualTypes;
	public TCFunctionType type;

	public TCExplicitFunctionDefinition expdef = null;
	public TCImplicitFunctionDefinition impdef = null;

	public TCFuncInstantiationExpression(TCExpression function, TCTypeList types)
	{
		super(function);
		this.function = function;
		this.actualTypes = types;
		this.unresolved = new TCTypeList();
		
		for (TCType type: types)
		{
			unresolved.addAll(type.unresolvedTypes());
		}
	}

	@Override
	public String toString()
	{
		return "(" + function + ")[" + Utils.listToString(actualTypes) + "]";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		// If there are no type qualifiers passed because the poly function value
		// is being accessed alone (not applied). In this case, the null qualifier
		// will cause TCVariableExpression to search for anything that matches the
		// name alone. If there is precisely one, it is selected; if there are
		// several, this is an ambiguity error.
		//
		// Note that a poly function is hard to identify from the actual types
		// passed here because the number of parameters may not equal the number
		// of type parameters.

		TCType ftype = function.typeCheck(env, qualifiers, scope, null);

		if (ftype.isUnknown(location))
		{
			return setType(ftype);
		}

		if (ftype.isFunction(location))
		{
			TCFunctionType t = ftype.getFunction();
			TCTypeSet set = new TCTypeSet();

			if (t.definitions == null)
			{
				report(3098, "Function value is not polymorphic");
				set.add(new TCUnknownType(location));
			}
			else
			{
    			boolean serious = (t.definitions.size() == 1);

    			for (TCDefinition def: t.definitions)		// Possibly a union of several
    			{
    				TCTypeList typeParams = null;
    				def = def.deref();

    				if (def instanceof TCExplicitFunctionDefinition)
    				{
    					expdef = (TCExplicitFunctionDefinition)def;
    					type = (TCFunctionType)expdef.getType();
    					typeParams = expdef.typeParams;
    				}
    				else if (def instanceof TCImplicitFunctionDefinition)
    				{
    					impdef = (TCImplicitFunctionDefinition)def;
    					type = (TCFunctionType)impdef.getType();
    					typeParams = impdef.typeParams;
    				}
    				else
    				{
    					report(3099, "Polymorphic function is not in scope");
    					continue;
    				}

    				if (typeParams == null)
    				{
    					concern(serious, 3100, "Function has no type parameters");
    					continue;
    				}

    				if (actualTypes.size() != typeParams.size())
    				{
    					concern(serious, 3101, "Expecting " + typeParams.size() + " type parameters");
    					continue;
    				}

    				TCTypeList fixed = new TCTypeList();
    				Map<TCNameToken, TCType> map = new HashMap<TCNameToken, TCType>();

    				for (int i=0; i < actualTypes.size(); i++)
    				{
    					TCType atype = actualTypes.get(i);
    					atype = atype.typeResolve(env);
    					fixed.add(atype);

    					TCParameterType ptype = (TCParameterType) typeParams.get(i);
    					
    					if (!TypeComparator.compatible(ptype.paramPattern, atype))
    					{
    						TCType act = actualTypes.get(i);
    						act.concern(serious, 3061, "Inappropriate type for parameter " + (i + 1));
    						
    						if (atype instanceof TCParameterType)
    						{
    							TCParameterType p = (TCParameterType)atype;
    							detail2(serious, "Expect", ptype.paramPattern, "Actual",
    								p.paramPattern == null ? atype : p.paramPattern);
    						}
    						else
    						{
    							detail2(serious, "Expect", ptype.paramPattern, "Actual", atype);
    						}
    					}

    					map.put(ptype.name, atype);
    					TypeComparator.checkComposeTypes(atype, env, false);
    				}

    				actualTypes = fixed;
    				type = (TCFunctionType)TCInstantiate.instantiate(type, map);

    				set.add(type);
    			}
    			
    			TypeComparator.checkImports(env, unresolved, location.module);
			}

			if (!set.isEmpty())
			{
				return setType(set.getType(location));
			}
		}
		else
		{
			report(3103, "Function instantiation does not yield a function");
		}

		return setType(new TCUnknownType(location));
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseFuncInstantiationExpression(this, arg);
	}
}
