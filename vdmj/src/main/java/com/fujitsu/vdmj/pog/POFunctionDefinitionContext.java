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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import java.util.Iterator;
import java.util.List;

import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingExpressionVisitor;
import com.fujitsu.vdmj.po.patterns.visitors.PORemoveIgnoresVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

public class POFunctionDefinitionContext extends POContext
{
	public final PODefinition definition;
	public final POAnnotationList annotations;
	public final TCNameToken name;
	public final TCFunctionType deftype;
	public final List<POPatternList> paramPatternList;
	public final boolean addPrecond;
	public final String precondition;
	public final TCTypeList typeParams;
	public final POExpression preExp;

	public POFunctionDefinitionContext(
		POExplicitFunctionDefinition definition, boolean precond)
	{
		this.definition = definition;
		this.annotations = definition.annotations;
		this.name = definition.name;
		this.deftype = definition.type;
		this.paramPatternList = definition.paramPatternList;
		this.addPrecond = precond;
		PORemoveIgnoresVisitor.init();
		this.precondition = preconditionCall(name, definition.typeParams, paramPatternList, definition.precondition);
		this.typeParams = definition.typeParams;
		this.preExp = definition.precondition;
	}

	public POFunctionDefinitionContext(
		POImplicitFunctionDefinition definition, boolean precond)
	{
		this.definition = definition;
		this.annotations = definition.annotations;
		this.name = definition.name;
		this.deftype = definition.type;
		this.addPrecond = precond;
		this.paramPatternList = definition.getParamPatternList();
		PORemoveIgnoresVisitor.init();
		this.precondition = preconditionCall(name, definition.typeParams, paramPatternList, definition.precondition);
		this.typeParams = definition.typeParams;
		this.preExp = definition.precondition;
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();
		POGetMatchingExpressionVisitor.init();

		if (!deftype.parameters.isEmpty())
		{
    		sb.append("forall ");
    		String sep = "";
    		TCFunctionType ftype = deftype;
    		PORemoveIgnoresVisitor.init();

    		for (POPatternList pl: paramPatternList)
    		{
    			Iterator<TCType> types = ftype.parameters.iterator();

    			for (POPattern p: pl)
    			{
					sb.append(sep);
					sb.append(p.removeIgnorePatterns());
					sb.append(":");
					TCType ptype = types.next();
					sb.append(ptype.toExplicitString(name.getLocation()));
					sep = ", ";
    			}

    			if (ftype.result instanceof TCFunctionType)
    			{
    				ftype = (TCFunctionType)ftype.result;
    			}
    			else
    			{
    				break;
    			}
    		}

    		sb.append(" &");
    		PORemoveIgnoresVisitor.init();

    		if (addPrecond && precondition != null)
    		{
    			sb.append(" ");
    			sb.append(precondition);
    			sb.append(" =>");
    		}
		}

		return sb.toString();
	}
	
	@Override
	public TCTypeList getTypeParams()
	{
		return typeParams;
	}
	
	@Override
	public POAnnotationList getAnnotations()
	{
		return annotations;
	}
	
	@Override
	public PODefinition getDefinition()
	{
		return definition;
	}
	
	@Override
	public TCNameSet reasonsAbout()
	{
		if (addPrecond && preExp != null)
		{
			return preExp.getVariableNames();
		}
		
		return super.reasonsAbout();
	}
}
