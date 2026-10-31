/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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

package smtlib.visitors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCAndExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCExpressionSet;
import com.fujitsu.vdmj.tc.expressions.TCLetDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

import smtlib.ast.And;
import smtlib.ast.Expression;

public class Utils implements ASTHelper
{
	public static TCIdentifierPattern checkPattern(TCPattern pattern, TCType type, TCExpressionList letquals, Environment env)
	{
		// eg. mk_R(a, -, b) becomes $R plus  "let a = $R.f1, b = $R.f3 in ..."

		TCIdentifierPattern pname = null;
		
		if (pattern instanceof TCIdentifierPattern)
		{
			pname = (TCIdentifierPattern)pattern;
		}
		else
		{
			String kind = pattern.getClass().getSimpleName();
			kind = kind.replace("TC", "").replace("Pattern", "");

			pname = LocalNames.pattern(
				kind + "_" + pattern.location.startLine + "_" + pattern.location.startPos);
		}

		PatternExpander expander = new PatternExpander();
		TCVariableExpression root = new TCVariableExpression(ANYWHERE, pname.name, pname.toString());
		root.setType(type);
		pattern.apply(expander, root);

		TreeMap<TCTypeBind,TCExpression> bindings = expander.getBindings();

		if (!bindings.isEmpty())
		{
			TCDefinitionList localDefs = new TCDefinitionList();

			for (Entry<TCTypeBind, TCExpression> entry: bindings.entrySet())
			{
				localDefs.add(new TCValueDefinition(
					NameScope.LOCAL,
					null,
					TCAccessSpecifier.DEFAULT,
					entry.getKey().pattern,
					entry.getKey().type,
					entry.getValue()));
			}

			letquals.add(new TCLetDefExpression(ANYWHERE, localDefs, ZERO));
		}

		return pname;
	}

	public static TCExpression combineQualifiers(TCExpressionList letquals, TCExpression body)
	{
		TCLetDefExpression result = null;

		for (TCExpression qual: letquals)
		{
			TCLetDefExpression letqual = (TCLetDefExpression)qual;

			if (result == null)
			{
				result = new TCLetDefExpression(ANYWHERE, letqual.localDefs, body);
			}
			else
			{
				result = new TCLetDefExpression(ANYWHERE, letqual.localDefs, result);
			}
		}

		return result;
	}
	
	public static TCExpression andOf(Collection<TCExpression> qualifiers)
	{
		Iterator<TCExpression> iter = qualifiers.iterator();
		TCExpression head = iter.next();

		if (!iter.hasNext())
		{
			return head;
		}

		TCExpressionSet tail = new TCExpressionSet();

		while (iter.hasNext())
		{
			tail.add(iter.next());
		}

		return new TCAndExpression(head, AND, andOf(tail));
	}

	public static Expression andOfAll(Collection<Expression> qualifiers)
	{
		Iterator<Expression> iter = qualifiers.iterator();
		Expression head = iter.next();

		if (!iter.hasNext())
		{
			return head;
		}

		Set<Expression> tail = new HashSet<Expression>();

		while (iter.hasNext())
		{
			tail.add(iter.next());
		}

		return new And(head, andOfAll(tail));
	}

	public static String expandOneLine(TCExpression exp)
	{
		return exp.toString().replace("\n", " ");
	}
}
