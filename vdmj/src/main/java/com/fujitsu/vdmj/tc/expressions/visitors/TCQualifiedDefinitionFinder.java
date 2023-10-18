/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.tc.expressions.visitors;

import com.fujitsu.vdmj.tc.annotations.TCAnnotatedExpression;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCQualifiedDefinition;
import com.fujitsu.vdmj.tc.expressions.TCAndExpression;
import com.fujitsu.vdmj.tc.expressions.TCEqualsExpression;
import com.fujitsu.vdmj.tc.expressions.TCEquivalentExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCInSetExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsExpression;
import com.fujitsu.vdmj.tc.expressions.TCNilExpression;
import com.fujitsu.vdmj.tc.expressions.TCNotEqualExpression;
import com.fujitsu.vdmj.tc.expressions.TCPreOpExpression;
import com.fujitsu.vdmj.tc.expressions.TCProperSubsetExpression;
import com.fujitsu.vdmj.tc.expressions.TCSubsetExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * Analyse an expression and return a set of QualifiedDefinitions which can be used
 * if the overall expression is true. This is used by the getQualifiedDefs method of TCExpression,
 * which in turn is used to typecheck the parts of the spec that apply when the value is
 * true.
 * 
 * The expression should already have been type checked.
 * 
 * For example, in "if is_real(a) then <exp of a> else 0" we can assume "a" is type real
 * within the "then" clause, and so the caseIsExpression below will create a QualifiedDefinition.
 * Note that it cannot be used in the "else" clause though. See TCIfExpression.
 * 
 * The cases where we can definitely qualify types like this is very limited.
 */
public class TCQualifiedDefinitionFinder extends TCExpressionVisitor<TCDefinitionList, Environment>
{
	@Override
	public TCDefinitionList caseExpression(TCExpression node, Environment env)
	{
		return new TCDefinitionList();
	}
	
	@Override
	public TCDefinitionList caseAnnotatedExpression(TCAnnotatedExpression node, Environment env)
	{
		return node.expression.apply(this, env);
	}
	
	@Override
	public TCDefinitionList caseAndExpression(TCAndExpression node, Environment env)
	{
		TCDefinitionList result = node.left.apply(this, env);
		result.addAll(node.right.apply(this, env));
		return result;
	}
	
	@Override
	public TCDefinitionList casePreOpExpression(TCPreOpExpression node, Environment env)
	{
		return node.expression.apply(this, env);
	}
	
	@Override
	public TCDefinitionList caseIsExpression(TCIsExpression node, Environment env)
	{
		TCDefinitionList result = new TCDefinitionList();
		
		if (node.test instanceof TCVariableExpression)
		{
			TCVariableExpression exp = (TCVariableExpression)node.test;
			// Lookup with any name type to avoid scope errors, but test NAMES below.
			TCDefinition existing = env.findName(exp.name, NameScope.NAMESANDANYSTATE);
			
			if (existing != null && existing.nameScope.matches(NameScope.NAMES))
			{
        		if (node.basictype != null)
        		{
       				result.add(new TCQualifiedDefinition(existing, node.basictype));
        		}
        		else if (node.typename != null)
        		{
        			if (node.typedef == null)
        			{
        				node.typedef = env.findType(node.typename, node.location.module);
        			}

        			if (node.typedef != null)
        			{
        				result.add(new TCQualifiedDefinition(existing, node.typedef.getType()));
        			}
        		}
			}
		}
		
		return result;
	}
	
	@Override
	public TCDefinitionList caseNotEqualExpression(TCNotEqualExpression node, Environment env)
	{
		TCDefinitionList result = new TCDefinitionList();
		
		if (node.left instanceof TCVariableExpression &&
			node.right instanceof TCNilExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;
			// Lookup with any name type to avoid scope errors, but test NAMES below.
			TCDefinition existing = env.findName(var.name, NameScope.NAMESANDANYSTATE);
			
			if (existing != null && existing.nameScope.matches(NameScope.NAMES) &&
				existing.getType() instanceof TCOptionalType)
			{
				// if var <> nil, var is the underlying type
				TCOptionalType optional = (TCOptionalType)existing.getType();
  				result.add(new TCQualifiedDefinition(existing, optional.type));
  			}
		}
		
		return result;		
	}
	
	@Override
	public TCDefinitionList caseEqualsExpression(TCEqualsExpression node, Environment env)
	{
		TCDefinitionList result = new TCDefinitionList();
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;
			// Lookup with any name type to avoid scope errors, but test NAMES below.
			TCDefinition existing = env.findName(var.name, NameScope.NAMESANDANYSTATE);
			
			if (existing != null && existing.nameScope.matches(NameScope.NAMES))
			{
				// if var = exp, var is the same type as exp
  				result.add(new TCQualifiedDefinition(existing, node.rtype));
  			}
		}
		
		return result;		
	}
	
	@Override
	public TCDefinitionList caseInSetExpression(TCInSetExpression node, Environment env)
	{
		TCDefinitionList result = new TCDefinitionList();
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;
			// Lookup with any name type to avoid scope errors, but test NAMES below.
			TCDefinition existing = env.findName(var.name, NameScope.NAMESANDANYSTATE);
			
			if (existing != null && existing.nameScope.matches(NameScope.NAMES))
			{
				// if var in set exp, var is the same type as exp elements
				if (node.rtype.isSet(node.location))
				{
					TCSetType set = node.rtype.getSet();
					result.add(new TCQualifiedDefinition(existing, set.setof));
				}
  			}
		}
		
		return result;		
	}

	@Override
	public TCDefinitionList caseEquivalentExpression(TCEquivalentExpression node, Environment env)
	{
		TCDefinitionList result = new TCDefinitionList();
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;
			// Lookup with any name type to avoid scope errors, but test NAMES below.
			TCDefinition existing = env.findName(var.name, NameScope.NAMESANDANYSTATE);
			
			if (existing != null && existing.nameScope.matches(NameScope.NAMES))
			{
				// if var <=> exp, var is boolean
				result.add(new TCQualifiedDefinition(existing, new TCBooleanType(node.location)));
  			}
		}
		
		return result;		
	}
	
	@Override
	public TCDefinitionList caseSubsetExpression(TCSubsetExpression node, Environment env)
	{
		TCDefinitionList result = new TCDefinitionList();
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;
			// Lookup with any name type to avoid scope errors, but test NAMES below.
			TCDefinition existing = env.findName(var.name, NameScope.NAMESANDANYSTATE);
			
			if (existing != null && existing.nameScope.matches(NameScope.NAMES))
			{
				// if var subset exp, var is the same type as the set (at most)
				if (node.rtype.isSet(node.location))
				{
					TCSetType set = node.rtype.getSet();
					result.add(new TCQualifiedDefinition(existing, set));
				}
  			}
		}
		
		return result;		
	}
	
	@Override
	public TCDefinitionList caseProperSubsetExpression(TCProperSubsetExpression node, Environment env)
	{
		TCDefinitionList result = new TCDefinitionList();
		
		if (node.left instanceof TCVariableExpression)
		{
			TCVariableExpression var = (TCVariableExpression)node.left;
			// Lookup with any name type to avoid scope errors, but test NAMES below.
			TCDefinition existing = env.findName(var.name, NameScope.NAMESANDANYSTATE);
			
			if (existing != null && existing.nameScope.matches(NameScope.NAMES))
			{
				// if var psubset exp, var is the same type as the set (at most)
				if (node.rtype.isSet(node.location))
				{
					TCSetType set = node.rtype.getSet();
					result.add(new TCQualifiedDefinition(existing, set));
				}
  			}
		}
		
		return result;		
	}
}
