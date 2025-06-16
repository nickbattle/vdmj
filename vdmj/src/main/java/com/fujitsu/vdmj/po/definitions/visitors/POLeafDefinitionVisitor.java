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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.definitions.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.POClassInvariantDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POEqualsDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POExternalDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImportedDefinition;
import com.fujitsu.vdmj.po.definitions.POInheritedDefinition;
import com.fujitsu.vdmj.po.definitions.POInstanceVariableDefinition;
import com.fujitsu.vdmj.po.definitions.POLocalDefinition;
import com.fujitsu.vdmj.po.definitions.POMultiBindListDefinition;
import com.fujitsu.vdmj.po.definitions.POMutexSyncDefinition;
import com.fujitsu.vdmj.po.definitions.PONamedTraceDefinition;
import com.fujitsu.vdmj.po.definitions.POPerSyncDefinition;
import com.fujitsu.vdmj.po.definitions.POQualifiedDefinition;
import com.fujitsu.vdmj.po.definitions.PORenamedDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.definitions.POThreadDefinition;
import com.fujitsu.vdmj.po.definitions.POTypeDefinition;
import com.fujitsu.vdmj.po.definitions.POUntypedDefinition;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;
import com.fujitsu.vdmj.po.patterns.POMultipleBind;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.statements.POErrorCase;
import com.fujitsu.vdmj.po.statements.POExternalClause;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCField;

/**
 * This PODefinition visitor visits all of the leaves of a definition tree and calls
 * the basic processing methods for the simple statements and expressions.
 */
abstract public class POLeafDefinitionVisitor<E, C extends Collection<E>, S> extends PODefinitionVisitor<C, S>
{
	protected POVisitorSet<E, C, S> visitorSet = new POVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			definitionVisitor = POLeafDefinitionVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return POLeafDefinitionVisitor.this.newCollection();
		}
	};

 	@Override
	public C caseAssignmentDefinition(POAssignmentDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.getType(), arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.expression, arg));
		return all;
	}

 	@Override
	public C caseClassDefinition(POClassDefinition node, S arg)
	{
 		C all = newCollection();
 		
 		for (PODefinition def: node.definitions)
 		{
 			all.addAll(def.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseClassInvariantDefinition(POClassInvariantDefinition node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.expression, arg);
	}

 	@Override
	public C caseEqualsDefinition(POEqualsDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.getType(), arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.test, arg));
		return all;
	}

 	@Override
	public C caseExplicitFunctionDefinition(POExplicitFunctionDefinition node, S arg)
	{
		C all = newCollection();
		
		for (POPatternList plist: node.paramPatternList)
		{
			for (POPattern p: plist)
			{
				all.addAll(visitorSet.applyPatternVisitor(p, arg));
			}
		}

		all.addAll(visitorSet.applyTypeVisitor(node.type, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.body, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		all.addAll(visitorSet.applyDefinitionVisitor(node.measureDef, arg));

		return all;
	}

 	@Override
	public C caseExplicitOperationDefinition(POExplicitOperationDefinition node, S arg)
	{
		C all = newCollection();
		
		for (POPattern p: node.parameterPatterns)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}

		all.addAll(visitorSet.applyTypeVisitor(node.type, arg));
		all.addAll(visitorSet.applyStatementVisitor(node.body, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		
		return all;
	}

	@Override
	public C caseExternalDefinition(POExternalDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseImplicitFunctionDefinition(POImplicitFunctionDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.getType(), arg);
		
		for (POPatternListTypePair ptp: node.parameterPatterns)
		{
			all.addAll(visitorSet.applyTypeVisitor(ptp.type, arg));

			for (POPattern p: ptp.patterns)
			{
				all.addAll(visitorSet.applyPatternVisitor(p, arg));
			}
		}

		all.addAll(visitorSet.applyPatternVisitor(node.result.pattern, arg));
		all.addAll(visitorSet.applyTypeVisitor(node.result.type, arg));
		
		all.addAll(visitorSet.applyExpressionVisitor(node.body, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		all.addAll(visitorSet.applyDefinitionVisitor(node.measureDef, arg));
		
		return all;
	}

 	@Override
	public C caseImplicitOperationDefinition(POImplicitOperationDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.getType(), arg);
		
		for (POPatternListTypePair ptp: node.parameterPatterns)
		{
			all.addAll(visitorSet.applyTypeVisitor(ptp.type, arg));

			for (POPattern p: ptp.patterns)
			{
				all.addAll(visitorSet.applyPatternVisitor(p, arg));
			}
		}

		if (node.result != null)
		{
			all.addAll(visitorSet.applyPatternVisitor(node.result.pattern, arg));
			all.addAll(visitorSet.applyTypeVisitor(node.result.type, arg));
		}
		
		if (node.externals != null)
		{
			for (POExternalClause ex: node.externals)
			{
				all.addAll(visitorSet.applyTypeVisitor(ex.type, arg));
			}
		}
		
		if (node.errors != null)
		{
			for (POErrorCase err: node.errors)
			{
				all.addAll(visitorSet.applyExpressionVisitor(err.left, arg));
				all.addAll(visitorSet.applyExpressionVisitor(err.right, arg));
			}
		}

		all.addAll(visitorSet.applyStatementVisitor(node.body, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		
		return all;
	}

 	@Override
	public C caseImportedDefinition(POImportedDefinition node, S arg)
	{
		return node.def.apply(this, arg);
	}

 	@Override
	public C caseInheritedDefinition(POInheritedDefinition node, S arg)
	{
		return node.superdef.apply(this, arg);
	}

 	@Override
	public C caseInstanceVariableDefinition(POInstanceVariableDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.type, arg);	
		all.addAll(visitorSet.applyExpressionVisitor(node.expression, arg));
		return all;
	}

 	@Override
	public C caseLocalDefinition(POLocalDefinition node, S arg)
	{
		return visitorSet.applyTypeVisitor(node.getType(), arg);
	}

 	@Override
	public C caseMultiBindListDefinition(POMultiBindListDefinition node, S arg)
	{
 		C all = newCollection();
 		
		for (POMultipleBind bind: node.bindings)
 		{
 			all.addAll(visitorSet.applyMultiBindVisitor(bind, arg));
 		}
		
		return all;
	}

 	@Override
	public C caseMutexSyncDefinition(POMutexSyncDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseNamedTraceDefinition(PONamedTraceDefinition node, S arg)
	{
		return newCollection();		// No POs in trace?
	}

 	@Override
	public C casePerSyncDefinition(POPerSyncDefinition node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.guard, arg);
	}

 	@Override
	public C caseQualifiedDefinition(POQualifiedDefinition node, S arg)
	{
		return node.def.apply(this, arg);
	}

 	@Override
	public C caseRenamedDefinition(PORenamedDefinition node, S arg)
	{
		return node.def.apply(this, arg);
	}

 	@Override
	public C caseStateDefinition(POStateDefinition node, S arg)
	{
		C all = newCollection();
		
		for (TCField field: node.fields)
		{
			all.addAll(visitorSet.applyTypeVisitor(field.type, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.invExpression, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.initExpression, arg));
		
		return all;
	}

 	@Override
	public C caseThreadDefinition(POThreadDefinition node, S arg)
	{
		return visitorSet.applyStatementVisitor(node.statement, arg);
	}

 	@Override
	public C caseTypeDefinition(POTypeDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.type, arg);
		all.addAll(visitorSet.applyPatternVisitor(node.invPattern, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.invExpression, arg));
		all.addAll(visitorSet.applyPatternVisitor(node.eqPattern1, arg));
		all.addAll(visitorSet.applyPatternVisitor(node.eqPattern2, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.eqExpression, arg));
		all.addAll(visitorSet.applyPatternVisitor(node.ordPattern1, arg));
		all.addAll(visitorSet.applyPatternVisitor(node.ordPattern2, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.ordExpression, arg));
		return all;
	}

 	@Override
	public C caseUntypedDefinition(POUntypedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseValueDefinition(POValueDefinition node, S arg)
	{
		C all = visitorSet.applyPatternVisitor(node.pattern, arg);
		all.addAll(visitorSet.applyTypeVisitor(node.type, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
		return all;
	}

	abstract protected C newCollection();
}
