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
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.visitors.TCImportExportVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * The parent class of all import declarations.
 */
abstract public class TCImport extends TCNode
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the import declaration. */
	public final LexLocation location;
	
	/** The name of the object being imported. */
	public final TCNameToken name;
	
	/** The renamed name of the object being imported. */
	public final TCNameToken renamed;
	
	/** The TCModule imported from. */
	public TCModule from;

	/**
	 * Create an import declaration for the given name(s).
	 */
	public TCImport(TCNameToken name, TCNameToken renamed)
	{
		this.location = name.getLocation();
		this.name = name;
		this.renamed = renamed;
	}

	@Override
	abstract public String toString();

	/** Extract the definition(s) for the import from the module passed. */
	abstract public TCDefinitionList getDefinitions(TCModule module);

	/** Check that the import types match the exported types. */
	abstract public void typeCheck(Environment env);
	
	/** Return the expected kind of the exported definition */
	abstract public boolean isExpectedKind(TCDefinition def);
	
	/** The kind of this import (type, value, function or operation) */
	abstract public String kind();
	
	/** Check the kind of the exported definition matches that expected. */
	protected void checkKind(TCDefinition actual)
	{
		if (actual != null && !isExpectedKind(actual))
		{
			report(3356, "Import of " + kind() + " " + name + " is " + actual.kind());
		}
	}

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
		return visitor.caseImport(this, arg);
	}
}
