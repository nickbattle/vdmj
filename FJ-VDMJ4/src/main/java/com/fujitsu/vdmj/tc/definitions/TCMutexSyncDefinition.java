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

package com.fujitsu.vdmj.tc.definitions;

import java.math.BigInteger;

import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCEqualsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCHistoryExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.util.Utils;

public class TCMutexSyncDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCNameList operations;

	public TCMutexSyncDefinition(TCAnnotationList annotations, LexLocation location, TCNameList operations)
	{
		super(Pass.DEFS, location, null, NameScope.GLOBAL);
		this.annotations = annotations;
		this.operations = operations;
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return new TCDefinitionList();
	}

	@Override
	public TCType getType()
	{
		return new TCUnknownType(location);
	}

	@Override
	public String toString()
	{
		return "mutex(" +
			(operations.isEmpty() ? "all)" :
				Utils.listToString("", operations, ", ", ")"));
	}
	
	@Override
	public String kind()
	{
		return "mutex";
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof TCMutexSyncDefinition)
		{
			return toString().equals(other.toString());
		}
		
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		return null;
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		if (annotations != null) annotations.tcBefore(this, base, scope);

		TCClassDefinition classdef = base.findClassDefinition();

		if (operations.isEmpty())
		{
			// Add all locally visibly mutexable operations for mutex(all)

			for (TCDefinition def: classdef.getLocalDefinitions())
			{
				if (def.isCallableOperation() &&
					!def.name.getName().equals(classdef.name.getName()) &&
					!def.isPure())
				{
					operations.add(def.name);
				}
			}
		}

		for (TCNameToken opname: operations)
		{
			int found = 0;

			for (TCDefinition def: classdef.getDefinitions())
			{
				if (def.name != null && def.name.matches(opname))
				{
					found++;

					if (!def.isCallableOperation())
					{
						opname.report(3038, opname + " is not an explicit operation");
					}
					
					if (def.isPure())
					{
						opname.report(3343, "Cannot have a mutex with pure operations");
					}
				}
			}

    		if (found == 0)
    		{
    			opname.report(3039, opname + " is not in scope");
    		}
    		else if (found > 1)
    		{
    			opname.warning(5002, "Mutex of overloaded operation");
    		}

    		if (opname.getName().equals(classdef.name.getName()))
    		{
    			opname.report(3040, "Cannot put mutex on a constructor");
    		}

    		for (TCNameToken other: operations)
    		{
    			if (opname != other && opname.equals(other))
    			{
    				opname.report(3041, "Duplicate mutex name");
    			}
    		}
		}

		if (annotations != null) annotations.tcAfter(this, getType(), base, scope);
	}

	public TCExpression getExpression(TCNameToken excluding)
	{
		TCNameList list = null;

		if (operations.size() == 1)
		{
			list = operations;
		}
		else
		{
			list = new TCNameList();
			list.addAll(operations);
			list.remove(excluding);
		}

		return new TCEqualsExpression(
			new TCHistoryExpression(location, Token.ACTIVE, list),
    		new LexKeywordToken(Token.EQUALS, location),
    		new TCIntegerLiteralExpression(new LexIntegerToken(BigInteger.ZERO, location)));
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMutexSyncDefinition(this, arg);
	}
}
