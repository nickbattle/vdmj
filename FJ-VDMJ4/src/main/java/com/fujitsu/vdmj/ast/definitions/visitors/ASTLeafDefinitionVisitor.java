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

package com.fujitsu.vdmj.ast.definitions.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.ast.ASTVisitorSet;
import com.fujitsu.vdmj.ast.definitions.ASTAssignmentDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassInvariantDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTEqualsDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExternalDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImportedDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTInheritedDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTInstanceVariableDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTLocalDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTMultiBindListDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTMutexSyncDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTNamedTraceDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTPerSyncDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTRenamedDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTStateDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTThreadDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTTypeDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTUntypedDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTValueDefinition;
import com.fujitsu.vdmj.ast.expressions.visitors.ASTExpressionVisitor;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleSeqBind;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleSetBind;
import com.fujitsu.vdmj.ast.statements.visitors.ASTStatementVisitor;
import com.fujitsu.vdmj.ast.types.ASTField;
import com.fujitsu.vdmj.ast.types.ASTFunctionType;
import com.fujitsu.vdmj.ast.types.ASTPatternListTypePair;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.ast.types.ASTTypeList;
import com.fujitsu.vdmj.ast.types.ASTUnknownType;
import com.fujitsu.vdmj.ast.types.visitors.ASTTypeVisitor;

/**
 * This ASTDefinition visitor visits all of the leaves of a definition tree and calls
 * the basic processing methods for the simple statements and expressions.
 */
abstract public class ASTLeafDefinitionVisitor<E, C extends Collection<E>, S> extends ASTDefinitionVisitor<C, S>
{
	protected ASTVisitorSet<E, C, S> visitorSet;

 	@Override
	public C caseAssignmentDefinition(ASTAssignmentDefinition node, S arg)
	{
		ASTExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		ASTTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.type.apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.expression.apply(expVisitor, arg));
		}
		
		return all;
	}

 	@Override
	public C caseClassDefinition(ASTClassDefinition node, S arg)
	{
 		C all = newCollection();
 		
 		for (ASTDefinition def: node.definitions)
 		{
 			all.addAll(def.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseClassInvariantDefinition(ASTClassInvariantDefinition node, S arg)
	{
		ASTExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		return (expVisitor != null ? node.expression.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseEqualsDefinition(ASTEqualsDefinition node, S arg)
	{
		ASTExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		ASTTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			ASTType type = node.typebind != null ? node.typebind.type : new ASTUnknownType(node.location);
			all.addAll(type.apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.test.apply(expVisitor, arg));
		}
		
		return all;
	}

 	@Override
	public C caseExplicitFunctionDefinition(ASTExplicitFunctionDefinition node, S arg)
	{
		ASTExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		ASTTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.type.apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.body.apply(expVisitor, arg));
		}
		
		return all;
	}

 	@Override
	public C caseExplicitOperationDefinition(ASTExplicitOperationDefinition node, S arg)
	{
		ASTStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
		ASTTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.type.apply(typeVisitor, arg));
		}
		
		if (stmtVisitor != null)
		{
			all.addAll(node.body.apply(stmtVisitor, arg));
		}
		
		return all;
	}

	@Override
	public C caseExternalDefinition(ASTExternalDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseImplicitFunctionDefinition(ASTImplicitFunctionDefinition node, S arg)
	{
 		if (node.body != null)
 		{
			ASTExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
			ASTTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
			C all = newCollection();
			
			if (typeVisitor != null)
			{
				ASTTypeList ptypes = new ASTTypeList();

				for (ASTPatternListTypePair ptp: node.parameterPatterns)
				{
					for (int i=0; i<ptp.patterns.size(); i++)
					{
						ptypes.add(ptp.type);
					}
				}

				// NB: implicit functions are always +> total, apparently
				ASTFunctionType type = new ASTFunctionType(node.location, false, ptypes, node.result.type);
				all.addAll(type.apply(typeVisitor, arg));
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
	public C caseImplicitOperationDefinition(ASTImplicitOperationDefinition node, S arg)
	{
 		if (node.body != null)
 		{
 			ASTStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
 			ASTTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
 			C all = newCollection();
 			
 			if (typeVisitor != null)
 			{
				ASTTypeList ptypes = new ASTTypeList();

				for (ASTPatternListTypePair ptp: node.parameterPatterns)
				{
					for (int i=0; i<ptp.patterns.size(); i++)
					{
						ptypes.add(ptp.type);
					}
				}

				// NB: implicit functions are always +> total, apparently
				ASTFunctionType type = new ASTFunctionType(node.location, false, ptypes, node.result.type);
				all.addAll(type.apply(typeVisitor, arg));
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
	public C caseImportedDefinition(ASTImportedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInheritedDefinition(ASTInheritedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInstanceVariableDefinition(ASTInstanceVariableDefinition node, S arg)
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
	public C caseLocalDefinition(ASTLocalDefinition node, S arg)
	{
		ASTTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.type.apply(typeVisitor, arg));
		}
		
		return all;
	}

 	@Override
	public C caseMultiBindListDefinition(ASTMultiBindListDefinition node, S arg)
	{
 		C all = newCollection();
 		
		for (ASTMultipleBind bind: node.bindings)
 		{
 			all.addAll(caseMultipleBind(bind, arg));
 		}
		
		return all;
	}

 	@Override
	public C caseMutexSyncDefinition(ASTMutexSyncDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseNamedTraceDefinition(ASTNamedTraceDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C casePerSyncDefinition(ASTPerSyncDefinition node, S arg)
	{
		ASTExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		return (expVisitor != null ? node.guard.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseRenamedDefinition(ASTRenamedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseStateDefinition(ASTStateDefinition node, S arg)
	{
		ASTExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		ASTTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			for (ASTField field: node.fields)
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
	public C caseThreadDefinition(ASTThreadDefinition node, S arg)
	{
		ASTStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
		return (stmtVisitor != null ? node.statement.apply(stmtVisitor, arg) : newCollection());
	}

 	@Override
	public C caseTypeDefinition(ASTTypeDefinition node, S arg)
	{
		ASTExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		ASTTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.type.apply(typeVisitor, arg));
		}
		
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
	public C caseUntypedDefinition(ASTUntypedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseValueDefinition(ASTValueDefinition node, S arg)
	{
		ASTExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		ASTTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.type.apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.exp.apply(expVisitor, arg));
		}
		
		return all;
	}

 	private C caseMultipleBind(ASTMultipleBind bind, S arg)
	{
		ASTExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			if (bind instanceof ASTMultipleSetBind)
			{
				ASTMultipleSetBind sbind = (ASTMultipleSetBind)bind;
				all.addAll(sbind.set.apply(expVisitor, arg));
			}
			else if (bind instanceof ASTMultipleSeqBind)
			{
				ASTMultipleSeqBind sbind = (ASTMultipleSeqBind)bind;
				all.addAll(sbind.sequence.apply(expVisitor, arg));
			}
		}
		
		return all;
	}
	
	abstract protected C newCollection();
}
