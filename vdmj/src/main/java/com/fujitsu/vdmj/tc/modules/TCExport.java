/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.tc.modules;

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.modules.visitors.TCImportExportVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * The parent class of all export declarations.
 */

abstract public class TCExport extends TCNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the export statement. */
	public final LexLocation location;

	/**
	 * Create an export declaration at the given location.
	 */

	public TCExport(LexLocation location)
	{
		this.location = location;
	}

	@Override
	abstract public String toString();

	/** Create a new definition for the export declaration. */
	abstract public TCDefinitionList getDefinition();

	/** Lookup the actual definition of the export declaration. */
	abstract public TCDefinitionList getDefinition(TCDefinitionList actualDefs);

	/** Check that the export types match the defined types. */
	abstract public void typeCheck(Environment env, TCDefinitionList actualDefs);

	public void report(int number, String msg)
	{
		TypeChecker.report(number, msg, location);
	}

	public void detail(String tag, Object obj)
	{
		TypeChecker.detail(tag, obj);
	}

	public void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		TypeChecker.detail2(tag1, obj1, tag2, obj2);
	}

	public <R, S> R apply(TCImportExportVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExport(this, arg);
	}
}
