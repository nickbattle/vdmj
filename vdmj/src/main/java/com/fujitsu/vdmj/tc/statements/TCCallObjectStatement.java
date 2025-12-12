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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexStringToken;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionListList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCStringLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.PrivateClassEnvironment;
import com.fujitsu.vdmj.typechecker.PublicClassEnvironment;
import com.fujitsu.vdmj.typechecker.TCRecursiveCycles;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class TCCallObjectStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCObjectDesignator designator;
	public final TCNameToken classname;
	public final TCIdentifierToken fieldname;
	public final TCExpressionList args;
	public final boolean explicit;

	public TCNameToken field = null;

	// Used by PO
	public TCDefinitionListList recursiveCycles = null;
	public TCDefinition fdef = null;

	public TCCallObjectStatement(TCObjectDesignator designator, TCNameToken classname, TCIdentifierToken fieldname, TCExpressionList args)
	{
		super(designator.location);

		this.designator = designator;
		this.classname = classname;
		this.fieldname = fieldname;
		this.args = args;
		this.explicit = (classname != null && classname.getModule() != null);
	}

	@Override
	public String toString()
	{
		return designator + "." +
			(classname != null ? classname : fieldname) +
			"(" + Utils.listToString(args) + ")";
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		TCType dtype = designator.typeCheck(env, null);

		if (!dtype.isClass(env))
		{
			report(3207, "Object designator is not an object type");
			return setType(new TCUnknownType(location));
		}

		TCClassType ctype = dtype.getClassType(env);
		TCClassDefinition classdef = ctype.classdef;
		TCClassDefinition self = env.findClassDefinition();
		Environment classenv = null;

		if (self == classdef || self.hasSupertype(classdef.getType()))
		{
			// All fields visible. Note that protected fields are inherited
			// into "locals" so they are effectively private
			classenv = new PrivateClassEnvironment(self);
		}
		else
		{
			// Only public fields externally visible
			classenv = new PublicClassEnvironment(classdef);
		}

		if (classname == null)
		{
			field = new TCNameToken(
				fieldname.getLocation(), ctype.name.getName(), fieldname.getName(), false, explicit);
		}
		else
		{
			field = classname;
		}

		TCTypeList atypes = getArgTypes(env, scope);
		field.setTypeQualifier(atypes);
		fdef = classenv.findName(field, scope);

		if (isConstructor(fdef) && !inConstructor(env))
		{
			report(3337, "Cannot call a constructor from here");
			return setType(new TCUnknownType(location));				
		}

		// Special code for the deploy method of CPU

		if (Settings.dialect == Dialect.VDM_RT &&
			field.getModule().equals("CPU") && field.getName().equals("deploy"))
		{
			if (!atypes.get(0).isType(TCClassType.class, location))
			{
				args.get(0).report(3280, "Argument to deploy must be an object");
			}

			return setType(new TCVoidType(location));
		}
		else if (Settings.dialect == Dialect.VDM_RT &&
			field.getModule().equals("CPU") && field.getName().equals("setPriority"))
		{
			if (!(atypes.get(0) instanceof TCOperationType))
			{
				args.get(0).report(3290, "Argument to setPriority must be an operation");
			}
			else
			{
				// Convert the variable expression to a string...
    			TCVariableExpression a1 = (TCVariableExpression)args.get(0);
    			args.remove(0);
    			args.add(0, new TCStringLiteralExpression(
    				new LexStringToken(
    					a1.name.getExplicit(true).getName(), a1.location)));

    			if (a1.name.getModule().equals(a1.name.getName()))	// it's a constructor
    			{
    				args.get(0).report(3291, "Argument to setPriority cannot be a constructor");
    			}
			}

			return setType(new TCVoidType(location));
		}
		else if (fdef == null)
		{
			// Use raw method, so we can use field's location
			TypeChecker.report(3209, "Member " + field + " is not in scope", field.getLocation());
			env.listAlternatives(field);
			return setType(new TCUnknownType(location));
		}
		else if (fdef.isStatic() && !env.isStatic())
		{
			// warning(5005, "Should invoke member " + field + " from a static context");
		}

		TCType type = fdef.getType();

		if (type.isOperation(location))
		{
			TCOperationType optype = type.getOperation();
			optype.typeResolve(env);
    		TCDefinition encldef = env.getEnclosingDefinition();
    		
    		if (encldef != null && encldef.isPure() && !optype.isPure())
    		{
    			report(3339, "Cannot call impure operation from a pure operation");
    		}

    		field.setTypeQualifier(optype.parameters);

			if (encldef != null && fdef != null)
			{
				recursiveCycles = new TCDefinitionListList();
				TCRecursiveCycles.getInstance().addCaller(encldef, recursiveCycles, fdef);
			}

			checkArgTypes(optype.parameters, atypes);	// Not necessary?
			return checkReturnType(constraint, optype.result, mandatory);
		}
		else if (type.isFunction(location))
		{
			// This is the case where a function is called as an operation without
			// a "return" statement.

			TCFunctionType ftype = type.getFunction();
			ftype.typeResolve(env);
			field.setTypeQualifier(ftype.parameters);
			checkArgTypes(ftype.parameters, atypes);	// Not necessary?
			return checkReturnType(constraint, ftype.result, mandatory);
		}
		else
		{
			report(3210, "Object member is neither a function nor an operation");
			return setType(new TCUnknownType(location));
		}
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
			report(3211, "Expecting " + ptypes.size() + " arguments");
		}
		else
		{
			int i=0;

			for (TCType atype: atypes)
			{
				TCType ptype = ptypes.get(i++);

				if (!TypeComparator.compatible(ptype, atype))
				{
					atype.report(3212, "Unexpected type for argument " + i);
					detail2("Expected", ptype, "Actual", atype);
				}
			}
		}
	}
	
	public TCDefinition getDefinition()
	{
		return fdef;	// Note that this is only set after typeCheck
	}
	
	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCallObjectStatement(this, arg);
	}
}
