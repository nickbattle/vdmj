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

package com.fujitsu.vdmj.in.definitions.visitors;

import com.fujitsu.vdmj.in.definitions.INAssignmentDefinition;
import com.fujitsu.vdmj.in.definitions.INBUSClassDefinition;
import com.fujitsu.vdmj.in.definitions.INCPUClassDefinition;
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
import com.fujitsu.vdmj.in.definitions.INSystemDefinition;
import com.fujitsu.vdmj.in.definitions.INThreadDefinition;
import com.fujitsu.vdmj.in.definitions.INTypeDefinition;
import com.fujitsu.vdmj.in.definitions.INUntypedDefinition;
import com.fujitsu.vdmj.in.definitions.INValueDefinition;

/**
 * The base type for all TCDefinition visitors. All methods, by default, call
 * the abstract caseDefinition method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class INDefinitionVisitor<R, S>
{
 	abstract public R caseDefinition(INDefinition node, S arg);

 	public R caseAssignmentDefinition(INAssignmentDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseBUSClassDefinition(INBUSClassDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseClassDefinition(INClassDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseClassInvariantDefinition(INClassInvariantDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseCPUClassDefinition(INCPUClassDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseEqualsDefinition(INEqualsDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExplicitFunctionDefinition(INExplicitFunctionDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExplicitOperationDefinition(INExplicitOperationDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExternalDefinition(INExternalDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImplicitFunctionDefinition(INImplicitFunctionDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImplicitOperationDefinition(INImplicitOperationDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImportedDefinition(INImportedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseInheritedDefinition(INInheritedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseInstanceVariableDefinition(INInstanceVariableDefinition node, S arg)
	{
		return caseAssignmentDefinition(node, arg);
	}

 	public R caseLocalDefinition(INLocalDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseMultiBindListDefinition(INMultiBindListDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseMutexSyncDefinition(INMutexSyncDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseNamedTraceDefinition(INNamedTraceDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R casePerSyncDefinition(INPerSyncDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseQualifiedDefinition(INQualifiedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

	public R caseRenamedDefinition(INRenamedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseStateDefinition(INStateDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseSystemDefinition(INSystemDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseThreadDefinition(INThreadDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseTypeDefinition(INTypeDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseUntypedDefinition(INUntypedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseValueDefinition(INValueDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}
}
