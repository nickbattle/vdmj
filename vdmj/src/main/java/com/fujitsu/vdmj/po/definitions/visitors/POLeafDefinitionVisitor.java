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
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.po.patterns.POMultipleBind;
import com.fujitsu.vdmj.po.patterns.POMultipleSeqBind;
import com.fujitsu.vdmj.po.patterns.POMultipleSetBind;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

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
			// None
		}

		@Override
		protected C newCollection()
		{
			return null;
		}
	};

 	@Override
	public C caseAssignmentDefinition(POAssignmentDefinition node, S arg)
	{
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.expression.apply(expVisitor, arg));
		}
		
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
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		return (expVisitor != null ? node.expression.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseEqualsDefinition(POEqualsDefinition node, S arg)
	{
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.test.apply(expVisitor, arg));
		}
		
		return all;
	}

 	@Override
	public C caseExplicitFunctionDefinition(POExplicitFunctionDefinition node, S arg)
	{
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.body.apply(expVisitor, arg));
		}
		
		if (node.predef != null)
		{
			all.addAll(node.predef.apply(this, arg));
		}
		
		if (node.postdef != null)
		{
			all.addAll(node.postdef.apply(this, arg));
		}
		
		if (node.measureDef != null)
		{
			all.addAll(node.measureDef.apply(this, arg));
		}

		return all;
	}

 	@Override
	public C caseExplicitOperationDefinition(POExplicitOperationDefinition node, S arg)
	{
		POStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (stmtVisitor != null)
		{
			all.addAll(node.body.apply(stmtVisitor, arg));
		}
		
		if (node.predef != null)
		{
			all.addAll(node.predef.apply(this, arg));
		}
		
		if (node.postdef != null)
		{
			all.addAll(node.postdef.apply(this, arg));
		}
		
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
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
 		if (node.body != null && expVisitor != null)
		{
			all.addAll(node.body.apply(expVisitor, arg));
		}

		if (node.predef != null)
		{
			all.addAll(node.predef.apply(this, arg));
		}
		
		if (node.postdef != null)
		{
			all.addAll(node.postdef.apply(this, arg));
		}
		
		if (node.measureDef != null)
		{
			all.addAll(node.measureDef.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseImplicitOperationDefinition(POImplicitOperationDefinition node, S arg)
	{
		POStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (stmtVisitor != null && node.body != null)
		{
			all.addAll(node.body.apply(stmtVisitor, arg));
		}

		if (node.predef != null)
		{
			all.addAll(node.predef.apply(this, arg));
		}
		
		if (node.postdef != null)
		{
			all.addAll(node.postdef.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseImportedDefinition(POImportedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInheritedDefinition(POInheritedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInstanceVariableDefinition(POInstanceVariableDefinition node, S arg)
	{
 		if (node.expression != null)
 		{
 			return caseAssignmentDefinition(node, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}

 	@Override
	public C caseLocalDefinition(POLocalDefinition node, S arg)
	{
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		return all;
	}

 	@Override
	public C caseMultiBindListDefinition(POMultiBindListDefinition node, S arg)
	{
 		C all = newCollection();
 		
		for (POMultipleBind bind: node.bindings)
 		{
 			all.addAll(caseMultipleBind(bind, arg));
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
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		return (expVisitor != null ? node.guard.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseQualifiedDefinition(POQualifiedDefinition node, S arg)
	{
		return node.def.apply(this, arg);
	}

 	@Override
	public C caseRenamedDefinition(PORenamedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseStateDefinition(POStateDefinition node, S arg)
	{
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			for (TCField field: node.fields)
			{
				all.addAll(field.type.apply(typeVisitor, arg));
			}
		}
		
		if (expVisitor != null)
		{
			if (node.invExpression != null)
			{
				all.addAll(node.invExpression.apply(expVisitor, arg));
			}

			if (node.initExpression != null)
			{
				all.addAll(node.initExpression.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseThreadDefinition(POThreadDefinition node, S arg)
	{
		POStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
		return (stmtVisitor != null ? node.statement.apply(stmtVisitor, arg) : newCollection());
	}

 	@Override
	public C caseTypeDefinition(POTypeDefinition node, S arg)
	{
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.type.apply(typeVisitor, arg));
		}
		
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
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.exp.apply(expVisitor, arg));
		}
		
		return all;
	}

 	private C caseMultipleBind(POMultipleBind bind, S arg)
	{
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			if (bind instanceof POMultipleSetBind)
			{
				POMultipleSetBind sbind = (POMultipleSetBind)bind;
				all.addAll(sbind.set.apply(expVisitor, arg));
			}
			else if (bind instanceof POMultipleSeqBind)
			{
				POMultipleSeqBind sbind = (POMultipleSeqBind)bind;
				all.addAll(sbind.sequence.apply(expVisitor, arg));
			}
		}
		
		return all;
	}
	
	abstract protected C newCollection();
}
