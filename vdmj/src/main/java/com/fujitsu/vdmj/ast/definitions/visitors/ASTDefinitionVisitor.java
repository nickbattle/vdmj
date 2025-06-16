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

package com.fujitsu.vdmj.ast.definitions.visitors;

import com.fujitsu.vdmj.ast.definitions.ASTAssignmentDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTBUSClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTCPUClassDefinition;
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
import com.fujitsu.vdmj.ast.definitions.ASTMultiBindListDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTMutexSyncDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTNamedTraceDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTPerSyncDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTRenamedDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTStateDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTSystemDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTThreadDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTTypeDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTUntypedDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTValueDefinition;

/**
 * The base type for all TCDefinition visitors. All methods, by default, call
 * the abstract caseDefinition method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class ASTDefinitionVisitor<R, S>
{
 	abstract public R caseDefinition(ASTDefinition node, S arg);

 	public R caseAssignmentDefinition(ASTAssignmentDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseBUSClassDefinition(ASTBUSClassDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseClassDefinition(ASTClassDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseClassInvariantDefinition(ASTClassInvariantDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseCPUClassDefinition(ASTCPUClassDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseEqualsDefinition(ASTEqualsDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExplicitFunctionDefinition(ASTExplicitFunctionDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExplicitOperationDefinition(ASTExplicitOperationDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExternalDefinition(ASTExternalDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImplicitFunctionDefinition(ASTImplicitFunctionDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImplicitOperationDefinition(ASTImplicitOperationDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImportedDefinition(ASTImportedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseInheritedDefinition(ASTInheritedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseInstanceVariableDefinition(ASTInstanceVariableDefinition node, S arg)
	{
		return caseAssignmentDefinition(node, arg);
	}

 	public R caseMultiBindListDefinition(ASTMultiBindListDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseMutexSyncDefinition(ASTMutexSyncDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseNamedTraceDefinition(ASTNamedTraceDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R casePerSyncDefinition(ASTPerSyncDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseRenamedDefinition(ASTRenamedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseStateDefinition(ASTStateDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseSystemDefinition(ASTSystemDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseThreadDefinition(ASTThreadDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseTypeDefinition(ASTTypeDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseUntypedDefinition(ASTUntypedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseValueDefinition(ASTValueDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}
}
