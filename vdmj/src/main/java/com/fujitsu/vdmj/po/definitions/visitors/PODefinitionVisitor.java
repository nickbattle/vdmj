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

package com.fujitsu.vdmj.po.definitions.visitors;

import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.POBUSClassDefinition;
import com.fujitsu.vdmj.po.definitions.POCPUClassDefinition;
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
import com.fujitsu.vdmj.po.definitions.POSystemDefinition;
import com.fujitsu.vdmj.po.definitions.POThreadDefinition;
import com.fujitsu.vdmj.po.definitions.POTypeDefinition;
import com.fujitsu.vdmj.po.definitions.POUntypedDefinition;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;

/**
 * The base type for all PODefinition visitors. All methods, by default, call
 * the abstract caseDefinition method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class PODefinitionVisitor<R, S>
{
 	abstract public R caseDefinition(PODefinition node, S arg);

 	public R caseAssignmentDefinition(POAssignmentDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseBUSClassDefinition(POBUSClassDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseClassDefinition(POClassDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseClassInvariantDefinition(POClassInvariantDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseCPUClassDefinition(POCPUClassDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseEqualsDefinition(POEqualsDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExplicitFunctionDefinition(POExplicitFunctionDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExplicitOperationDefinition(POExplicitOperationDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseExternalDefinition(POExternalDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImplicitFunctionDefinition(POImplicitFunctionDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImplicitOperationDefinition(POImplicitOperationDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseImportedDefinition(POImportedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseInheritedDefinition(POInheritedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseInstanceVariableDefinition(POInstanceVariableDefinition node, S arg)
	{
		return caseAssignmentDefinition(node, arg);
	}

 	public R caseLocalDefinition(POLocalDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseMultiBindListDefinition(POMultiBindListDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseMutexSyncDefinition(POMutexSyncDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseNamedTraceDefinition(PONamedTraceDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R casePerSyncDefinition(POPerSyncDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseQualifiedDefinition(POQualifiedDefinition node, S arg)
	{
 		return caseDefinition(node, arg);
	}

	public R caseRenamedDefinition(PORenamedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseStateDefinition(POStateDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseSystemDefinition(POSystemDefinition node, S arg)
	{
		return caseClassDefinition(node, arg);
	}

 	public R caseThreadDefinition(POThreadDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseTypeDefinition(POTypeDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseUntypedDefinition(POUntypedDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}

 	public R caseValueDefinition(POValueDefinition node, S arg)
	{
		return caseDefinition(node, arg);
	}
}
