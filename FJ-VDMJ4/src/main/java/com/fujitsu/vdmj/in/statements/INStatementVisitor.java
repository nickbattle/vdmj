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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.annotations.INAnnotatedStatement;

/**
 * The base type for all INStatement visitors. All methods, by default, call
 * the abstract caseStatement method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class INStatementVisitor<R, S>
{
 	abstract public R caseStatement(INStatement node, S arg);

	public R caseAnnotatedStatement(INAnnotatedStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

	public R caseAlwaysStatement(INAlwaysStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseAssignmentStatement(INAssignmentStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseAtomicStatement(INAtomicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseBlockStatement(INBlockStatement node, S arg)
	{
		return caseSimpleBlockStatement(node, arg);
	}

 	public R caseCallObjectStatement(INCallObjectStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCallStatement(INCallStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCasesStatement(INCasesStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseClassInvariantStatement(INClassInvariantStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseCyclesStatement(INCyclesStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseDefStatement(INDefStatement node, S arg)
	{
		return caseLetDefStatement(node, arg);
	}

 	public R caseDurationStatement(INDurationStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseElseIfStatement(INElseIfStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseErrorStatement(INErrorStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseExitStatement(INExitStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForAllStatement(INForAllStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForIndexStatement(INForIndexStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseForPatternBindStatement(INForPatternBindStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseIfStatement(INIfStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseLetBeStStatement(INLetBeStStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseLetDefStatement(INLetDefStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseNonDeterministicStatement(INNonDeterministicStatement node, S arg)
	{
		return caseSimpleBlockStatement(node, arg);
	}

 	public R caseNotYetSpecifiedStatement(INNotYetSpecifiedStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R casePeriodicStatement(INPeriodicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseReturnStatement(INReturnStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSimpleBlockStatement(INSimpleBlockStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSkipStatement(INSkipStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSpecificationStatement(INSpecificationStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSporadicStatement(INSporadicStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseStartStatement(INStartStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseStopStatement(INStopStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseSubclassResponsibilityStatement(INSubclassResponsibilityStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseTixeStatement(INTixeStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseTrapStatement(INTrapStatement node, S arg)
	{
		return caseStatement(node, arg);
	}

 	public R caseWhileStatement(INWhileStatement node, S arg)
	{
		return caseStatement(node, arg);
	}
}