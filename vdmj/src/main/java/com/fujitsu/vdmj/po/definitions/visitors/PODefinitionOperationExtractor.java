/*******************************************************************************
 *
 *	Copyright (c) 2025 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.po.definitions.visitors;

import java.util.LinkedHashMap;

import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POEqualsDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionOperationExtractor;
import com.fujitsu.vdmj.po.expressions.visitors.POOperationExtractionException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * Do operation extraction for any definition that can occur in the "localDefs" of
 * a "let" or "def".
 */
public class PODefinitionOperationExtractor extends PODefinitionVisitor<PODefinition, Object>
{
	private final POExpressionOperationExtractor expExtractor;

	public PODefinitionOperationExtractor(POExpressionOperationExtractor expExtractor)
	{
		this.expExtractor = expExtractor;
	}

	public PODefinitionOperationExtractor()
	{
		this(new POExpressionOperationExtractor());
	}

	public LinkedHashMap<TCNameToken, POApplyExpression> getSubstitutions()
	{
		return expExtractor.getSubstitutions();
	}

	@Override
	public PODefinition caseExplicitFunctionDefinition(POExplicitFunctionDefinition node, Object arg)
	{
		return node;	// Unchanged
	}

	@Override
	public PODefinition caseImplicitFunctionDefinition(POImplicitFunctionDefinition node, Object arg)
	{
		return node;	// Unchanged
	}

	@Override
	public PODefinition caseEqualsDefinition(POEqualsDefinition node, Object arg)
	{
		return new POEqualsDefinition(
			node.location,
			node.pattern,
			node.typebind,
			node.bind,
			node.test.apply(expExtractor),
			node.expType,
			node.defType,
			node.defs);
	}

	@Override
	public PODefinition caseAssignmentDefinition(POAssignmentDefinition node, Object arg)
	{
		return new POAssignmentDefinition(
			node.name,
			node.type,
			node.expression.apply(expExtractor),
			node.expType);
	}

	@Override
	public PODefinition caseValueDefinition(POValueDefinition node, Object arg)
	{
		return new POValueDefinition(
			node.annotations,
			node.pattern,
			node.type,
			node.exp.apply(expExtractor),
			node.expType,
			node.defs);
	}

	@Override
	public PODefinition caseDefinition(PODefinition node, Object arg)
	{
		throw new POOperationExtractionException(node, "Unsupported definition?");
	}
}
