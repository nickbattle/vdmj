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

package com.fujitsu.vdmj.tc.modules.visitors;

import com.fujitsu.vdmj.tc.modules.TCExport;
import com.fujitsu.vdmj.tc.modules.TCExportAll;
import com.fujitsu.vdmj.tc.modules.TCExportedFunction;
import com.fujitsu.vdmj.tc.modules.TCExportedOperation;
import com.fujitsu.vdmj.tc.modules.TCExportedType;
import com.fujitsu.vdmj.tc.modules.TCExportedValue;
import com.fujitsu.vdmj.tc.modules.TCImport;
import com.fujitsu.vdmj.tc.modules.TCImportAll;
import com.fujitsu.vdmj.tc.modules.TCImportedFunction;
import com.fujitsu.vdmj.tc.modules.TCImportedOperation;
import com.fujitsu.vdmj.tc.modules.TCImportedType;
import com.fujitsu.vdmj.tc.modules.TCImportedValue;

/**
 * The base type for all module import/export visitors.
 */
public abstract class TCImportExportVisitor<R, S>
{
 	abstract public R caseExport(TCExport node, S arg);
 	abstract public R caseImport(TCImport node, S arg);

 	public R caseExportAll(TCExportAll node, S arg)
	{
		return caseExport(node, arg);
	}

 	public R caseExportedFunction(TCExportedFunction node, S arg)
	{
		return caseExport(node, arg);
	}

 	public R caseExportedOperation(TCExportedOperation node, S arg)
	{
		return caseExport(node, arg);
	}

 	public R caseExportedType(TCExportedType node, S arg)
	{
		return caseExport(node, arg);
	}

 	public R caseExportedValue(TCExportedValue node, S arg)
	{
		return caseExport(node, arg);
	}
 	
 	// And imports...

 	public R caseImportAll(TCImportAll node, S arg)
	{
		return caseImport(node, arg);
	}

 	public R caseImportedFunction(TCImportedFunction node, S arg)
	{
		return caseImport(node, arg);
	}

 	public R caseImportedOperation(TCImportedOperation node, S arg)
	{
		return caseImport(node, arg);
	}

 	public R caseImportedType(TCImportedType node, S arg)
	{
		return caseImport(node, arg);
	}

 	public R caseImportedValue(TCImportedValue node, S arg)
	{
		return caseImport(node, arg);
	}
}
