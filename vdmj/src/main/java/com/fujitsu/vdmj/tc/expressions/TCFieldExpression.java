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

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;

public class TCFieldExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression object;
	public final TCIdentifierToken field;
	public TCNameToken memberName;
	public TCType root;

	public TCFieldExpression(TCExpression object, TCIdentifierToken field, TCNameToken memberName)
	{
		super(object);
		this.object = object;
		this.field = field;
		this.memberName = memberName;
	}

	@Override
	public String toString()
	{
		return "(" + object + "." +
			(memberName == null ? field.getName() : memberName.getName()) + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		root = object.typeCheck(env, null, scope, null);

		if (root instanceof TCUnknownType)
		{
			memberName = new TCNameToken(field.getLocation(), "?", "?", true);
			return setType(root);
		}

		TCTypeSet results = new TCTypeSet();
		boolean recOrClass = false;
		boolean unique = !root.isUnion(location);

		if (root.isRecord(location))
		{
    		TCRecordType rec = root.getRecord();
    		TCField cf = rec.findField(field.getName());
    		
    		// Check for state access via state record
    		TCStateDefinition state = env.findStateDefinition();
    		
    		if (state != null && state.getType().equals(rec))
    		{
    			TCNameToken sname = new TCNameToken(field.getLocation(), rec.name.getModule(), field.getName());
    			state.findName(sname, NameScope.STATE);		// Lookup marks as used
    		}

   			if (cf != null)
   			{
   				results.add(cf.type);
    		}
   			else
   			{
   				field.concern(unique,
   					3090, "Unknown field " + field.getName() + " in record " + rec.name);
   			}

   			recOrClass = true;
		}

		if (env.isVDMPP() && root.isClass(env))
		{
    		TCClassType cls = root.getClassType(env);

    		if (memberName == null)
    		{
    			memberName = cls.getMemberName(field);
    		}

    		memberName.setTypeQualifier(qualifiers);
    		
    		if (env.isFunctional())
    		{
    			scope = NameScope.VARSANDNAMES;		// Allow fields as well in functions
    		}
    		
    		TCDefinition fdef = cls.findName(memberName, scope);

   			if (fdef == null)
   			{
    			// The field may be a map or sequence, which would not
    			// have the type qualifier of its arguments in the name...

    			TCTypeList oldq = memberName.getTypeQualifier();
    			memberName.setTypeQualifier(null);
    			fdef = cls.findName(memberName, scope);
    			memberName.setTypeQualifier(oldq);	// Just for error text!
    		}

			if (fdef == null && memberName.getTypeQualifier() == null)
			{
				// We might be selecting a bare function or operation, without
				// applying it (ie. no qualifiers). In this case, if there is
				// precisely one possibility, we choose it.

				for (TCDefinition possible: env.findMatches(memberName))
				{
					if (possible.isFunctionOrOperation())
					{
						if (fdef != null)
						{
							fdef = null;	// Alas, more than one
							break;
						}
						else
						{
							fdef = possible;
						}
					}
				}
			}

			if (fdef == null)
			{
				field.concern(unique,
					3091, "Unknown member " + memberName + " of class " + cls.name.getName());

				if (unique)
				{
					env.listAlternatives(memberName);
				}
			}
			else if (TCClassDefinition.isAccessible(env, fdef, false))
   			{
				// The following gives lots of warnings for self.value access
				// to values as though they are fields of self in the CSK test
				// suite, so commented out for now.

				if (fdef.isStatic())// && !env.isStatic())
				{
					// warning(5005, "Should access member " + field + " from a static context");
				}
				
				// If we're trying to access a state field - probably an instance variable - and
				// the enclosing definition is not pure, we can only do this from non-functional
				// environments, like regular statements and so on. Functional contexts are not
				// allowed.
				
				if (fdef.nameScope.matches(NameScope.STATE) && Settings.release == Release.VDM_10)
				{
					TCDefinition encl = env.getEnclosingDefinition();
					
					if (!encl.isPure())
					{
						if (env.isFunctionalError())
						{
							TypeChecker.report(3366,
								"Cannot access state field '" + field + "' from this context", field.getLocation());
						}
						else if (env.isFunctional())
						{
							TypeChecker.warning(3366,
								"Should not access state field '" + field + "' from this context", field.getLocation());
						}
					}
				}

   				results.add(fdef.getType());
   				// At runtime, type qualifiers must match exactly
   				memberName.setTypeQualifier(fdef.name.getTypeQualifier());
    		}
   			else
   			{
   				field.concern(unique,
   					3092, "Inaccessible member " + memberName + " of class " + cls.name.getName());
   			}

   			recOrClass = true;
		}

		if (results.isEmpty())
		{
			if (root instanceof TCProductType)
			{
				object.report(3093, "Field '" + field.getName() + "' applied to tuple type (use #n)");
			}
			else if (!recOrClass)
    		{
    			if (root instanceof TCRecordType && ((TCRecordType)root).isOpaque(location))
    			{
    				object.report(3093, "Field '" + field.getName() + "' applied to non-struct export");
    			}
    			else
    			{
    				object.report(3093, "Field '" + field.getName() + "' applied to non-aggregate type");
    			}
    		}

    		return setType(new TCUnknownType(location));
		}

		return possibleConstraint(constraint, results.getType(location));
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseFieldExpression(this, arg);
	}
}
