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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.typechecker;

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * The parent class of all type checking environments.
 */
abstract public class Environment
{
	/** The environment chain. */
	protected final Environment outer;

	/** The enclosing func/op definition at this point, or null. */
	private TCDefinition enclosingDefinition = null;
	
	/** Whether we are in a functional or operational context. */
	private Boolean isFunctional = null;
	
	/** Whether a functional context should generate errors or warnings for operation calls. */
	private Boolean isFunctionalError = false;

	/**
	 * Create an environment linking to the given outer chain.
	 * @param outer
	 */

	public Environment(Environment outer)
	{
		this.outer = outer;
	}

	/**
	 * Check whether the list of definitions passed contains any duplicates,
	 * or whether any names in the list hide the same name further down the
	 * environment chain.
	 *
	 * @param list	The list of definitions to check.
	 */

	protected void dupHideCheck(TCDefinitionList list, NameScope scope)
	{
		TCNameList allnames = list.getVariableNames();

		for (TCNameToken n1: allnames)
		{
			TCNameList done = new TCNameList();

			for (TCNameToken n2: allnames)
			{
				if (n1 != n2 && n1.equals(n2) && !done.contains(n1))
				{
					TypeChecker.warning(5007, "Duplicate definition: " + n1, n1.getLocation());
					done.add(n1);
				}
			}

			if (outer != null)
			{
				// We search for any scoped name (ie. the first), but then check
				// the scope matches what we can see. If we pass scope to findName
				// it throws errors if the name does not match the scope.

				TCDefinition def = outer.findName(n1, NameScope.NAMESANDSTATE);

				if (def != null && def.location != n1.getLocation() &&
					def.nameScope.matches(scope))
				{
					// Reduce clutter for names in the same module/class
					String message = null;

					if (def.location.file.equals(n1.getLocation().file))
					{
						message = def.name.getName() + " " + def.location.toShortString() +
							" hidden by " +	n1.toString();
					}
					else
					{
						message = def.name.getName() + " " + def.location +
							" hidden by " + n1.toString();
					}

					TypeChecker.warning(5008, message, n1.getLocation());
				}
			}
		}
	}

	public TCDefinition getEnclosingDefinition()
	{
		if (enclosingDefinition != null)
		{
			return enclosingDefinition;
		}

		return outer == null ? null : outer.getEnclosingDefinition();
	}

	public void setEnclosingDefinition(TCDefinition def)
	{
		enclosingDefinition = def;
	}
	
	public boolean isFunctional()
	{
		if (isFunctional != null)
		{
			return isFunctional;
		}

		return outer == null ? false : outer.isFunctional();
	}
	
	public boolean isFunctionalError()
	{
		if (isFunctional != null)	// Only valid when isFunctional set
		{
			return isFunctionalError;
		}

		return outer == null ? false : outer.isFunctionalError();
	}
	
	public void setFunctional(Boolean functional, Boolean errors)
	{
		isFunctional = functional;

		if (functional != null && functional)
		{
			isFunctionalError = errors;
		}
		else
		{
			isFunctionalError = false;
		}
	}

	/** Find a name in the environment of the given scope. */
	abstract public TCDefinition findName(TCNameToken name, NameScope scope);

	/** Find a type in the environment. */
	abstract public TCDefinition findType(TCNameToken name, String fromModule);

	/** Find the state defined in the environment, if any. */
	abstract public TCStateDefinition findStateDefinition();

	/** Find the enclosing class definition, if any. */
	abstract public TCClassDefinition findClassDefinition();

	/** True if the calling context is a static function or operation. */
	abstract public boolean isStatic();

	/** Check whether any definitions in the environment were unused. */
	abstract public void unusedCheck();

	/** True if this is a VDM++ environment. */
	abstract public boolean isVDMPP();

	/** True if this is a VDM-RT "system" environment. */
	abstract public boolean isSystem();

	/** Find functions and operations of the given basic name. */
	abstract public TCDefinitionSet findMatches(TCNameToken name);

	/** Mark all definitions, at this level, used. */
	public void markUsed()
	{
		// Nothing, by default. Implemented in flat environments.
	}

	/** Add details to a TC error with alternative fn/op name possibilities. */
	public void listAlternatives(TCNameToken name)
	{
		for (TCDefinition possible: findMatches(name))
		{
			if (possible.isFunctionOrOperation())
			{
				TypeChecker.detail("Possible", possible.name);
			}
		}
	}

	/** Unravelling unused check. */
	public void unusedCheck(Environment downTo)
	{
		Environment p = this;

		while (p != null && p != downTo)
		{
			p.unusedCheck();
			p = p.outer;
		}
	}
}
