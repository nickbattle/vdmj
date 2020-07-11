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

package com.fujitsu.vdmj.in.definitions.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.definitions.INAssignmentDefinition;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INClassInvariantDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INEqualsDefinition;
import com.fujitsu.vdmj.in.definitions.INExplicitFunctionDefinition;
import com.fujitsu.vdmj.in.definitions.INExplicitOperationDefinition;
import com.fujitsu.vdmj.in.definitions.INExternalDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitFunctionDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitOperationDefinition;
import com.fujitsu.vdmj.in.definitions.INImportedDefinition;
import com.fujitsu.vdmj.in.definitions.INInheritedDefinition;
import com.fujitsu.vdmj.in.definitions.INInstanceVariableDefinition;
import com.fujitsu.vdmj.in.definitions.INLocalDefinition;
import com.fujitsu.vdmj.in.definitions.INMultiBindListDefinition;
import com.fujitsu.vdmj.in.definitions.INMutexSyncDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.in.definitions.INPerSyncDefinition;
import com.fujitsu.vdmj.in.definitions.INQualifiedDefinition;
import com.fujitsu.vdmj.in.definitions.INRenamedDefinition;
import com.fujitsu.vdmj.in.definitions.INStateDefinition;
import com.fujitsu.vdmj.in.definitions.INThreadDefinition;
import com.fujitsu.vdmj.in.definitions.INTypeDefinition;
import com.fujitsu.vdmj.in.definitions.INUntypedDefinition;
import com.fujitsu.vdmj.in.definitions.INValueDefinition;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INMultipleSeqBind;
import com.fujitsu.vdmj.in.patterns.INMultipleSetBind;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

/**
 * This INDefinition visitor visits all of the leaves of a definition tree and calls
 * the basic processing methods for the simple statements and expressions.
 */
abstract public class INLeafDefinitionVisitor<E, C extends Collection<E>, S> extends INDefinitionVisitor<C, S>
{
	protected INVisitorSet<E, C, S> visitorSet;

 	@Override
	public C caseAssignmentDefinition(INAssignmentDefinition node, S arg)
	{
		INExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
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
	public C caseClassDefinition(INClassDefinition node, S arg)
	{
 		C all = newCollection();
 		
 		for (INDefinition def: node.definitions)
 		{
 			all.addAll(def.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseClassInvariantDefinition(INClassInvariantDefinition node, S arg)
	{
		INExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		return (expVisitor != null ? node.expression.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseEqualsDefinition(INEqualsDefinition node, S arg)
	{
		INExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
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
	public C caseExplicitFunctionDefinition(INExplicitFunctionDefinition node, S arg)
	{
		INExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
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
		
		return all;
	}

 	@Override
	public C caseExplicitOperationDefinition(INExplicitOperationDefinition node, S arg)
	{
		INStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
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
		
		return all;
	}

	@Override
	public C caseExternalDefinition(INExternalDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseImplicitFunctionDefinition(INImplicitFunctionDefinition node, S arg)
	{
 		if (node.body != null)
 		{
			INExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
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
			
			return all;
 		}
 		else
 		{
 			return newCollection();
 		}
	}

 	@Override
	public C caseImplicitOperationDefinition(INImplicitOperationDefinition node, S arg)
	{
 		if (node.body != null)
 		{
 			INStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
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
 			
 			return all;
 		}
 		else
 		{
 			return newCollection();
 		}
	}

 	@Override
	public C caseImportedDefinition(INImportedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInheritedDefinition(INInheritedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInstanceVariableDefinition(INInstanceVariableDefinition node, S arg)
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
	public C caseLocalDefinition(INLocalDefinition node, S arg)
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
	public C caseMultiBindListDefinition(INMultiBindListDefinition node, S arg)
	{
 		C all = newCollection();
 		
		for (INMultipleBind bind: node.bindings)
 		{
 			all.addAll(caseMultipleBind(bind, arg));
 		}
		
		return all;
	}

 	@Override
	public C caseMutexSyncDefinition(INMutexSyncDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseNamedTraceDefinition(INNamedTraceDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C casePerSyncDefinition(INPerSyncDefinition node, S arg)
	{
		INExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		return (expVisitor != null ? node.guard.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseQualifiedDefinition(INQualifiedDefinition node, S arg)
	{
		return node.def.apply(this, arg);
	}

 	@Override
	public C caseRenamedDefinition(INRenamedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseStateDefinition(INStateDefinition node, S arg)
	{
		INExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
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
	public C caseThreadDefinition(INThreadDefinition node, S arg)
	{
		INStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
		return (stmtVisitor != null ? node.statement.apply(stmtVisitor, arg) : newCollection());
	}

 	@Override
	public C caseTypeDefinition(INTypeDefinition node, S arg)
	{
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.type.apply(typeVisitor, arg));
		}
		
		if (node.invdef != null)
		{
			all.addAll(node.invdef.apply(this, arg));
		}

		if (node.eqdef != null)
		{
			all.addAll(node.eqdef.apply(this, arg));
		}

		if (node.orddef != null)
		{
			all.addAll(node.orddef.apply(this, arg));
		}

		if (node.mindef != null)
		{
			all.addAll(node.mindef.apply(this, arg));
		}

		if (node.maxdef != null)
		{
			all.addAll(node.maxdef.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseUntypedDefinition(INUntypedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseValueDefinition(INValueDefinition node, S arg)
	{
		INExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
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

 	private C caseMultipleBind(INMultipleBind bind, S arg)
	{
		INExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			if (bind instanceof INMultipleSetBind)
			{
				INMultipleSetBind sbind = (INMultipleSetBind)bind;
				all.addAll(sbind.set.apply(expVisitor, arg));
			}
			else if (bind instanceof INMultipleSeqBind)
			{
				INMultipleSeqBind sbind = (INMultipleSeqBind)bind;
				all.addAll(sbind.sequence.apply(expVisitor, arg));
			}
		}
		
		return all;
	}
	
	abstract protected C newCollection();
}
