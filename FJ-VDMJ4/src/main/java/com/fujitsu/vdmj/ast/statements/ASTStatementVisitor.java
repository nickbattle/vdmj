/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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

package com.fujitsu.vdmj.ast.statements;

import com.fujitsu.vdmj.ast.annotations.ASTAnnotatedStatement;

/**
 * The base type for all ASTStatement visitors. All methods, by default, call
 * the abstract caseStatement method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class ASTStatementVisitor<R, S>
{
 	abstract public R caseStatement(ASTStatement node, S arg);

	public R caseAnnotatedStatement(ASTAnnotatedStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

	public R caseAlwaysStatement(ASTAlwaysStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseAssignmentStatement(ASTAssignmentStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseAtomicStatement(ASTAtomicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseBlockStatement(ASTBlockStatement node, S arg)
	{
		return caseSimpleBlockStatement(node, arg);
	}

 	public R caseCallObjectStatement(ASTCallObjectStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCallStatement(ASTCallStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCasesStatement(ASTCasesStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseClassInvariantStatement(ASTClassInvariantStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCyclesStatement(ASTCyclesStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseDefStatement(ASTDefStatement node, S arg)
	{
		return caseLetDefStatement(node, arg);
	}

 	public R caseDurationStatement(ASTDurationStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseElseIfStatement(ASTElseIfStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseErrorStatement(ASTErrorStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseExitStatement(ASTExitStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForAllStatement(ASTForAllStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForIndexStatement(ASTForIndexStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForPatternBindStatement(ASTForPatternBindStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseIfStatement(ASTIfStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseLetBeStStatement(ASTLetBeStStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseLetDefStatement(ASTLetDefStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseNonDeterministicStatement(ASTNonDeterministicStatement node, S arg)
	{
		return caseSimpleBlockStatement(node, arg);
	}

 	public R caseNotYetSpecifiedStatement(ASTNotYetSpecifiedStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R casePeriodicStatement(ASTPeriodicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseReturnStatement(ASTReturnStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSimpleBlockStatement(ASTSimpleBlockStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSkipStatement(ASTSkipStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSpecificationStatement(ASTSpecificationStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSporadicStatement(ASTSporadicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseStartStatement(ASTStartStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseStopStatement(ASTStopStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSubclassResponsibilityStatement(ASTSubclassResponsibilityStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseTixeStatement(ASTTixeStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseTrapStatement(ASTTrapStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseWhileStatement(ASTWhileStatement node, S arg)
	{
		return caseStatement(node, arg);
	}
}