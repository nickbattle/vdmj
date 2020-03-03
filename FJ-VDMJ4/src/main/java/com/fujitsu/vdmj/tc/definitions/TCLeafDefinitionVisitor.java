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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.definitions;

import java.util.Collection;

import com.fujitsu.vdmj.tc.expressions.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.statements.TCLeafStatementVisitor;

/**
 * This TCDefinitionVisitor visits all of the leaves of a definition tree and calls
 * the basic processing methods for the simple statements and expressions.
 */
abstract public class TCLeafDefinitionVisitor<E, C extends Collection<E>, S> extends TCDefinitionVisitor<C, S>
{
 	@Override
	public C caseAssignmentDefinition(TCAssignmentDefinition node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.expression.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseClassDefinition(TCClassDefinition node, S arg)
	{
 		C all = newCollection();
 		
 		for (TCDefinition def: node.definitions)
 		{
 			all.addAll(def.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseClassInvariantDefinition(TCClassInvariantDefinition node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.expression.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseEqualsDefinition(TCEqualsDefinition node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.test.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseExplicitFunctionDefinition(TCExplicitFunctionDefinition node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.body.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseExplicitOperationDefinition(TCExplicitOperationDefinition node, S arg)
	{
		TCLeafStatementVisitor<E, C, S> stmtVisitor = getStatementVisitor();
		return (stmtVisitor != null ? node.body.apply(stmtVisitor, arg) : newCollection());
	}

	@Override
	public C caseExternalDefinition(TCExternalDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseImplicitFunctionDefinition(TCImplicitFunctionDefinition node, S arg)
	{
 		if (node.body != null)
 		{
			TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
			return (expVisitor != null ? node.body.apply(expVisitor, arg) : newCollection());
 		}
 		else
 		{
 			return newCollection();
 		}
	}

 	@Override
	public C caseImplicitOperationDefinition(TCImplicitOperationDefinition node, S arg)
	{
 		if (node.body != null)
 		{
 			TCLeafStatementVisitor<E, C, S> stmtVisitor = getStatementVisitor();
 			return (stmtVisitor != null ? node.body.apply(stmtVisitor, arg) : newCollection());
 		}
 		else
 		{
 			return newCollection();
 		}
	}

 	@Override
	public C caseImportedDefinition(TCImportedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInheritedDefinition(TCInheritedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInstanceVariableDefinition(TCInstanceVariableDefinition node, S arg)
	{
 		if (node.expression != null)
 		{
			TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
			return (expVisitor != null ? node.expression.apply(expVisitor, arg) : newCollection());
 		}
 		else
 		{
 			return newCollection();
 		}
	}

 	@Override
	public C caseLocalDefinition(TCLocalDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseMultiBindListDefinition(TCMultiBindListDefinition node, S arg)
	{
 		C all = newCollection();
 		
		for (TCMultipleBind bind: node.bindings)
 		{
 			all.addAll(caseMultipleBind(bind, arg));
 		}
		
		return all;
	}

 	@Override
	public C caseMutexSyncDefinition(TCMutexSyncDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseNamedTraceDefinition(TCNamedTraceDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C casePerSyncDefinition(TCPerSyncDefinition node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.guard.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseQualifiedDefinition(TCQualifiedDefinition node, S arg)
	{
		return node.deref().apply(this, arg);
	}

 	@Override
	public C caseRenamedDefinition(TCRenamedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseStateDefinition(TCStateDefinition node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
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
	public C caseThreadDefinition(TCThreadDefinition node, S arg)
	{
		TCLeafStatementVisitor<E, C, S> stmtVisitor = getStatementVisitor();
		return (stmtVisitor != null ? node.statement.apply(stmtVisitor, arg) : newCollection());
	}

 	@Override
	public C caseTypeDefinition(TCTypeDefinition node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			if (node.invExpression != null)
			{
				all.addAll(node.invExpression.apply(expVisitor, arg));
			}

			if (node.eqExpression != null)
			{
				all.addAll(node.eqExpression.apply(expVisitor, arg));
			}

			if (node.ordExpression != null)
			{
				all.addAll(node.ordExpression.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseUntypedDefinition(TCUntypedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseValueDefinition(TCValueDefinition node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.exp.apply(expVisitor, arg) : newCollection());
	}

 	private C caseMultipleBind(TCMultipleBind bind, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			if (bind instanceof TCMultipleSetBind)
			{
				TCMultipleSetBind sbind = (TCMultipleSetBind)bind;
				all.addAll(sbind.set.apply(expVisitor, arg));
			}
			else if (bind instanceof TCMultipleSeqBind)
			{
				TCMultipleSeqBind sbind = (TCMultipleSeqBind)bind;
				all.addAll(sbind.sequence.apply(expVisitor, arg));
			}
		}
		
		return all;
	}
	
	abstract protected C newCollection();

 	protected TCLeafExpressionVisitor<E, C, S> getExpressionVisitor()
 	{
 		return null;	// Don't process expressions
 	}

 	protected TCLeafStatementVisitor<E, C, S> getStatementVisitor()
 	{
 		return null;	// Don't process statements
 	}
}
