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

package com.fujitsu.vdmj.tc.statements.visitors;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.definitions.TCAssignmentDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCEqualsDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCBindExitChecker;
import com.fujitsu.vdmj.tc.patterns.visitors.TCBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCMultipleBindExitChecker;
import com.fujitsu.vdmj.tc.patterns.visitors.TCMultipleBindVisitor;
import com.fujitsu.vdmj.tc.statements.TCBlockStatement;
import com.fujitsu.vdmj.tc.statements.TCCallObjectStatement;
import com.fujitsu.vdmj.tc.statements.TCCallStatement;
import com.fujitsu.vdmj.tc.statements.TCCaseStmtAlternative;
import com.fujitsu.vdmj.tc.statements.TCCasesStatement;
import com.fujitsu.vdmj.tc.statements.TCExitStatement;
import com.fujitsu.vdmj.tc.statements.TCLetBeStStatement;
import com.fujitsu.vdmj.tc.statements.TCLetDefStatement;
import com.fujitsu.vdmj.tc.statements.TCNotYetSpecifiedStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.TCSubclassResponsibilityStatement;
import com.fujitsu.vdmj.tc.statements.visitors.TCLeafStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCExitChecker extends TCLeafStatementVisitor<TCType, TCTypeSet, Environment>
{
	private static class VisitorSet extends TCVisitorSet<TCType, TCTypeSet, Environment>
	{
		private final TCLeafExpressionVisitor<TCType, TCTypeSet, Environment> expVisitor;
		private final TCStatementVisitor<TCTypeSet, Environment> stmtVisitor;
		private final TCBindVisitor<TCTypeSet, Environment> bindVisitor;
		private final TCMultipleBindVisitor<TCTypeSet, Environment> mbindVisitor;

		public VisitorSet(TCExitChecker parent)
		{
			expVisitor = new com.fujitsu.vdmj.tc.expressions.visitors.TCExitChecker(this);
			bindVisitor = new TCBindExitChecker(this);
			mbindVisitor = new TCMultipleBindExitChecker(this);
			stmtVisitor = parent;
		}

		@Override
		public TCLeafExpressionVisitor<TCType, TCTypeSet, Environment> getExpressionVisitor()
		{
			return expVisitor;
		}
		
		@Override
		public TCStatementVisitor<TCTypeSet, Environment> getStatementVisitor()
		{
			return stmtVisitor;
		}
		
		@Override
		public TCBindVisitor<TCTypeSet, Environment> getBindVisitor()
		{
			return bindVisitor;
		}
		
		@Override
		public TCMultipleBindVisitor<TCTypeSet, Environment> getMultiBindVisitor()
		{
			return mbindVisitor;
		}
	}

	public TCExitChecker()
	{
		visitorSet = new VisitorSet(this);
	}

	@Override
	protected TCTypeSet newCollection()
	{
		return new TCTypeSet();
	}

	@Override
	public TCTypeSet caseStatement(TCStatement node, Environment base)
	{
		return new TCTypeSet();
	}
	
	@Override
	public TCTypeSet caseBlockStatement(TCBlockStatement node, Environment base)
	{
		TCTypeSet types = caseSimpleBlockStatement(node, base);
		
		for (TCDefinition d: node.assignmentDefs)
		{
			if (d instanceof TCAssignmentDefinition)
			{
				TCAssignmentDefinition ad = (TCAssignmentDefinition)d;
				types.addAll(ad.expression.apply(visitorSet.getExpressionVisitor(), base));
			}
		}
		
		return types;
	}
	
	@Override
	public TCTypeSet caseCallObjectStatement(TCCallObjectStatement node, Environment base)
	{
		TCTypeSet result = new TCTypeSet();
		
		for (TCExpression arg : node.args)
		{
			result.addAll(arg.apply(visitorSet.getExpressionVisitor(), base));
		}

		boolean overridable = Settings.dialect != Dialect.VDM_SL &&
				node.fdef != null && !node.fdef.accessSpecifier.access.equals(Token.PRIVATE);

		if (node.fdef != null && !overridable)
		{
			if (node.fdef instanceof TCExplicitOperationDefinition)
			{
				TCExplicitOperationDefinition explop = (TCExplicitOperationDefinition)node.fdef;
				
				if (explop.possibleExceptions == null)
				{
					explop.possibleExceptions = TCDefinition.IN_PROGRESS;
					explop.possibleExceptions = explop.body.apply(visitorSet.getStatementVisitor(), base);
				}
				
				result.addAll(explop.possibleExceptions);
				return result;
			}
			else if (node.fdef instanceof TCImplicitOperationDefinition)
			{
				TCImplicitOperationDefinition implop = (TCImplicitOperationDefinition)node.fdef;
				
				if (implop.possibleExceptions == null)
				{
					if (implop.body != null)
					{
						implop.possibleExceptions = TCDefinition.IN_PROGRESS;
						implop.possibleExceptions = implop.body.apply(visitorSet.getStatementVisitor(), base);
					}
					else
					{
						return new TCTypeSet();
					}
				}
				
				result.addAll(implop.possibleExceptions);
				return result;
			}
		}

		result.add(new TCUnknownType(node.location));
		return result;
	}
	
	@Override
	public TCTypeSet caseCallStatement(TCCallStatement node, Environment base)
	{
		TCTypeSet result = new TCTypeSet();
		
		for (TCExpression arg : node.args)
		{
			result.addAll(arg.apply(visitorSet.getExpressionVisitor(), base));
		}


		TCDefinition opdef = base.findName(node.name, NameScope.GLOBAL);
		boolean overridable = Settings.dialect != Dialect.VDM_SL &&
				opdef != null && !opdef.accessSpecifier.access.equals(Token.PRIVATE);

		if (opdef != null && !overridable)
		{
			if (opdef instanceof TCExplicitOperationDefinition)
			{
				TCExplicitOperationDefinition explop = (TCExplicitOperationDefinition)opdef;
				
				if (explop.possibleExceptions == null)
				{
					explop.possibleExceptions = TCDefinition.IN_PROGRESS;
					explop.possibleExceptions = explop.body.apply(visitorSet.getStatementVisitor(), base);
				}
				
				result.addAll(explop.possibleExceptions);
				return result;
			}
			else if (opdef instanceof TCImplicitOperationDefinition)
			{
				TCImplicitOperationDefinition implop = (TCImplicitOperationDefinition)opdef;
				
				if (implop.possibleExceptions == null)
				{
					if (implop.body != null)
					{
						implop.possibleExceptions = TCDefinition.IN_PROGRESS;
						implop.possibleExceptions = implop.body.apply(visitorSet.getStatementVisitor(), base);
					}
					else
					{
						return new TCTypeSet();
					}
				}
				
				result.addAll(implop.possibleExceptions);
				return result;
			}
		}

		result.add(new TCUnknownType(node.location));
		return result;
	}
	
	@Override
	public TCTypeSet caseCasesStatement(TCCasesStatement node, Environment base)
	{
		TCTypeSet types = node.exp.apply(visitorSet.getExpressionVisitor(), base);

		for (TCCaseStmtAlternative c: node.cases)
		{
			types.addAll(c.statement.apply(this, base));
		}

		return types;
	}
	
	@Override
	public TCTypeSet caseExitStatement(TCExitStatement node, Environment arg)
	{
		TCTypeSet types = new TCTypeSet();

		if (node.expression == null)
		{
			types.add(new TCVoidType(node.location));
		}
		else if (node.exptype == null)	// Not yet checked
		{
			types.add(new TCUnknownType(node.location));
		}
		else
		{
			types.add(node.exptype);
		}

		return types;
	}
	
	@Override
	public TCTypeSet caseLetBeStStatement(TCLetBeStStatement node, Environment base)
	{
		TCTypeSet result = node.bind.apply(visitorSet.getMultiBindVisitor(), base);
		if (node.suchThat != null)
		{
			result.addAll(node.suchThat.apply(visitorSet.getExpressionVisitor(), base));
		}
		
		if (node.def != null)
		{
			for (TCDefinition d: node.def.getDefinitions())
			{
				if (d instanceof TCValueDefinition)
				{
					TCValueDefinition vd = (TCValueDefinition)d;
					result.addAll(vd.exp.apply(visitorSet.getExpressionVisitor(), base));
				}
			}
		}
		
		result.addAll(node.statement.apply(this, base));
		return result;
	}
	
	@Override
	public TCTypeSet caseLetDefStatement(TCLetDefStatement node, Environment base)
	{
		TCTypeSet result = new TCTypeSet();
		
		for (TCDefinition d: node.localDefs)
		{
			if (d instanceof TCEqualsDefinition)
			{
				TCEqualsDefinition ed = (TCEqualsDefinition)d;
				result.addAll(ed.test.apply(visitorSet.getExpressionVisitor(), base));
			}
			else if (d instanceof TCValueDefinition)
			{
				TCValueDefinition vd = (TCValueDefinition)d;
				result.addAll(vd.exp.apply(visitorSet.getExpressionVisitor(), base));
			}
		}
		
		result.addAll(node.statement.apply(this, base));
		return result;
	}
	
	@Override
	public TCTypeSet caseNotYetSpecifiedStatement(TCNotYetSpecifiedStatement node, Environment arg)
	{
		return new TCTypeSet(new TCUnknownType(node.location));
	}
	
	@Override
	public TCTypeSet caseSubclassResponsibilityStatement(TCSubclassResponsibilityStatement node, Environment arg)
	{
		return new TCTypeSet(new TCUnknownType(node.location));
	}
}
