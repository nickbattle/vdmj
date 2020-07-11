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

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCBUSClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCCPUClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCSystemDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Utils;

public class TCNewExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCIdentifierToken classname;
	public final TCExpressionList args;

	public TCClassDefinition classdef;
	public TCDefinition ctordef;

	public TCNewExpression(LexLocation location, TCIdentifierToken classname, TCExpressionList args)
	{
		super(location);
		this.classname = classname;
		this.args = args;
	}

	@Override
	public String toString()
	{
		return "new " + classname + "("+ Utils.listToString(args) + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCDefinition cdef = env.findType(classname.getClassName(), null);

		if (cdef == null || !(cdef instanceof TCClassDefinition))
		{
			report(3133, "Class name " + classname + " not in scope");
			return new TCUnknownType(location);
		}
		
		if (Settings.release == Release.VDM_10 && env.isFunctional())
		{
			report(3348, "Cannot use 'new' in a functional context");
		}

		classdef = (TCClassDefinition)cdef;

		if (classdef instanceof TCSystemDefinition)
		{
			report(3279, "Cannot instantiate system class " + classdef.name);
		}
		
		if (classdef.isAbstract)
		{
			report(3330, "Cannot instantiate abstract class " + classdef.name);
			
			for (TCDefinition d: classdef.getLocalDefinitions())
			{
				if (d.isSubclassResponsibility())
				{
					detail("Unimplemented", d.name.getName() + d.getType());
				}
			}
		}

		TCTypeList argtypes = new TCTypeList();

		for (TCExpression a: args)
		{
			argtypes.add(a.typeCheck(env, null, scope, null));
		}

		TCDefinition opdef = classdef.findConstructor(argtypes);

		if (opdef == null)
		{
			if (!args.isEmpty())	// Not having a default ctor is OK
    		{
    			report(3134, "Class has no constructor with these parameter types");
    			detail("Called", classdef.getCtorName(argtypes));
    		}
			else if (classdef instanceof TCCPUClassDefinition ||
					 classdef instanceof TCBUSClassDefinition)
			{
				report(3297, "Cannot use default constructor for this class");
			}
		}
		else
		{
			if (!opdef.isCallableOperation())
    		{
    			report(3135, "Class has no constructor with these parameter types");
    			detail("Called", classdef.getCtorName(argtypes));
    		}
			else if (!TCClassDefinition.isAccessible(env, opdef, false))
			{
    			report(3292, "Constructor is not accessible");
    			detail("Called", classdef.getCtorName(argtypes));
			}
			else
			{
				ctordef = opdef;
			}
		}

		return checkConstraint(constraint, classdef.getType());
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNewExpression(this, arg);
	}
}
