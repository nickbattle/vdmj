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

package com.fujitsu.vdmj.tc.definitions.visitors;

import com.fujitsu.vdmj.tc.definitions.TCAssignmentDefinition;
import com.fujitsu.vdmj.tc.definitions.TCBUSClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCCPUClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassInvariantDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCEqualsDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExternalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImportedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCInheritedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCInstanceVariableDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMultiBindListDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMutexSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCNamedTraceDefinition;
import com.fujitsu.vdmj.tc.definitions.TCPerSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCQualifiedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCRenamedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCSystemDefinition;
import com.fujitsu.vdmj.tc.definitions.TCThreadDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.definitions.TCUntypedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;

/**
 * The base type for all TCDefinition visitors. All methods, by default, call
 * the abstract caseDefinition method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class TCDefinitionVisitor<R, S>
{
 	abstract public R caseDefinition(TCDefinition node, S arg);

 	public R caseAssignmentDefinition(TCAssignmentDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseBUSClassDefinition(TCBUSClassDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseClassDefinition(TCClassDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseClassInvariantDefinition(TCClassInvariantDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseCPUClassDefinition(TCCPUClassDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseEqualsDefinition(TCEqualsDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExplicitFunctionDefinition(TCExplicitFunctionDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExplicitOperationDefinition(TCExplicitOperationDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExternalDefinition(TCExternalDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImplicitFunctionDefinition(TCImplicitFunctionDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImplicitOperationDefinition(TCImplicitOperationDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImportedDefinition(TCImportedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseInheritedDefinition(TCInheritedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseInstanceVariableDefinition(TCInstanceVariableDefinition node, S arg)
	{
		return caseAssignmentDefinition(node, arg);
	}

 	public R caseLocalDefinition(TCLocalDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseMultiBindListDefinition(TCMultiBindListDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseMutexSyncDefinition(TCMutexSyncDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseNamedTraceDefinition(TCNamedTraceDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R casePerSyncDefinition(TCPerSyncDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseQualifiedDefinition(TCQualifiedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseRenamedDefinition(TCRenamedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseStateDefinition(TCStateDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseSystemDefinition(TCSystemDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseThreadDefinition(TCThreadDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseTypeDefinition(TCTypeDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseUntypedDefinition(TCUntypedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseValueDefinition(TCValueDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}
}
