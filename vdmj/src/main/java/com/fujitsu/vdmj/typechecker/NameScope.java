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

package com.fujitsu.vdmj.typechecker;

/**
 * An enum to represent name scoping.
 */
public enum NameScope
{
	/**
	 * Definitions are declared with one of the following scopes.
	 */
	LOCAL(1),			// Let definitions and parameters
	GLOBAL(2),		// Module and class func/ops/values
	STATE(4),			// SL state or object instance values
	OLDSTATE(8),		// SL state names with a "~" modifier
	TYPENAME(16),		// The names of types
	CLASSNAME(32),	// The names of classes
	GHOST(64),		// Ghost variables used in @LoopInvariants

	/**
	 * Lookups of names in the environment specify one of the following "scope sets"
	 * which indicates which of the definitions (above) are applicable (usually
	 * to typechecking). For example, function bodies are typechecked as NAMES,
	 * since locals and globals in scope, but state isn't; operations are checked
	 * as NAMESANDSTATE, and postconditions as NAMESANDANYSTATE.
	 */
	NAMES(1+2),
	NAMESANDSTATE(1+2+4),
	NAMESANDANYSTATE(1+2+4+8),
	GHOSTSNAMESANDSTATE(1+2+4+64),
	
	ANYTHING(255);

	private int mask;

	NameScope(int level)
	{
		this.mask = level;
	}

	public boolean matches(NameScope other)
	{
		return (mask & other.mask) != 0;
	}
}
