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

package com.fujitsu.vdmj.tc.statements;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class TCCallStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken name;
	public final TCExpressionList args;

	public TCCallStatement(TCNameToken name, TCExpressionList args)
	{
		super(name.getLocation());
		this.name = name;
		this.args = args;
	}

	@Override
	public String toString()
	{
		return name.getName() + "(" + Utils.listToString(args) + ")";
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		TCTypeList atypes = getArgTypes(env, scope);

		if (env.isVDMPP())
		{
			name.setTypeQualifier(atypes);
		}

		TCDefinition opdef = env.findName(name, scope);

		if (opdef == null)
		{
			report(3213, "Operation " + name + " is not in scope");
			env.listAlternatives(name);
			return new TCUnknownType(location);
		}

		if (env.isVDMPP() && name.isExplicit())
		{
			// A call like X`op() is local if X is in our hierarchy
			// else it's a static call of a different class.
			
			TCClassDefinition self = env.findClassDefinition();
			TCType ctype = opdef.classDefinition.getType();
			
			if (!self.hasSupertype(ctype) && !opdef.isStatic())
			{
				report(3324, "Operation " + name + " is not static");
				return new TCUnknownType(location);				
			}
		}
		
		if (isConstructor(opdef) && !inConstructor(env))
		{
			report(3337, "Cannot call a constructor from here");
			return new TCUnknownType(location);				
		}

		if (!opdef.isStatic() && env.isStatic())
		{
			report(3214, "Cannot call " + name + " from static context");
			return new TCUnknownType(location);
		}

		TCType type = opdef.getType();

		if (type.isOperation(location))
		{
    		TCOperationType optype = type.getOperation();
    		optype.typeResolve(env, null);
    		TCDefinition encl = env.getEnclosingDefinition();
    		
    		if (encl != null && encl.isPure() && !optype.isPure())
    		{
    			report(3339, "Cannot call impure operation from a pure operation");
    		}

    		// Reset the name's qualifier with the actual operation type so
    		// that runtime search has a simple TypeComparator call.

    		if (env.isVDMPP())
    		{
    			name.setTypeQualifier(optype.parameters);
    		}

    		checkArgTypes(optype.parameters, atypes);
    		return checkReturnType(constraint, optype.result, mandatory);
		}
		else if (type.isFunction(location))
		{
			// This is the case where a function is called as an operation without
			// a "return" statement.

    		TCFunctionType ftype = type.getFunction();
    		ftype.typeResolve(env, null);

    		// Reset the name's qualifier with the actual function type so
    		// that runtime search has a simple TypeComparator call.

    		if (env.isVDMPP())
    		{
    			name.setTypeQualifier(ftype.parameters);
    		}

    		checkArgTypes(ftype.parameters, atypes);
    		return checkReturnType(constraint, ftype.result, mandatory);
		}
		else
		{
			report(3210, "Name is neither a function nor an operation");
			return new TCUnknownType(location);
		}
	}

	@Override
	public TCTypeSet exitCheck(Environment base)
	{
		TCDefinition opdef = base.findName(name, NameScope.GLOBAL);
		boolean overridable = Settings.dialect == Dialect.VDM_PP &&
				opdef != null && !opdef.accessSpecifier.access.equals(Token.PRIVATE);

		if (opdef != null && !overridable)
		{
			if (opdef instanceof TCExplicitOperationDefinition)
			{
				TCExplicitOperationDefinition explop = (TCExplicitOperationDefinition)opdef;
				
				if (explop.possibleExceptions == null)
				{
					explop.possibleExceptions = TCDefinition.IN_PROGRESS;
					explop.possibleExceptions = explop.body.exitCheck(base);
				}
				
				return explop.possibleExceptions;
			}
			else if (opdef instanceof TCImplicitOperationDefinition)
			{
				TCImplicitOperationDefinition implop = (TCImplicitOperationDefinition)opdef;
				
				if (implop.possibleExceptions == null)
				{
					if (implop.body != null)
					{
						implop.possibleExceptions = TCDefinition.IN_PROGRESS;
						implop.possibleExceptions = implop.body.exitCheck(base);
					}
					else
					{
						return new TCTypeSet();
					}
				}
				
				return implop.possibleExceptions;
			}
		}

		return new TCTypeSet(new TCUnknownType(location));
	}

	private TCTypeList getArgTypes(Environment env, NameScope scope)
	{
		TCTypeList types = new TCTypeList();

		for (TCExpression a: args)
		{
			types.add(a.typeCheck(env, null, scope, null));
		}

		return types;
	}

	private void checkArgTypes(TCTypeList ptypes, TCTypeList atypes)
	{
		if (ptypes.size() != atypes.size())
		{
			report(3216, "Expecting " + ptypes.size() + " arguments");
		}
		else
		{
			int i=0;

			for (TCType atype: atypes)
			{
				TCType ptype = ptypes.get(i++);

				if (!TypeComparator.compatible(ptype, atype))
				{
					report(3217, "Unexpected type for argument " + i);
					detail2("Expected", ptype, "Actual", atype);
				}
			}
		}
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env, AtomicBoolean returns)
	{
		TCNameSet names = new TCNameSet(name.getExplicit(true));
		names.addAll(args.getFreeVariables(globals, env));
		return names;
	}
}
