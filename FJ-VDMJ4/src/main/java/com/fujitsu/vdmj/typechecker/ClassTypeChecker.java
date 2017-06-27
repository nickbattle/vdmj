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
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCSystemDefinition;

/**
 * A class to coordinate all class type checking processing.
 */
public class ClassTypeChecker extends TypeChecker
{
	/** The list of classes to check. */
	private final TCClassList classes;

	/**
	 * Create a type checker with the list of classes passed.
	 *
	 * @param classes
	 */
	public ClassTypeChecker(TCClassList classes)
	{
		super();
		this.classes = classes;
	}

	/**
	 * Perform type checking across all classes in the list.
	 */
	@Override
	public void typeCheck()
	{
		boolean hasSystem = false;

		for (TCClassDefinition c1: classes)
		{
			for (TCClassDefinition c2: classes)
			{
				if (c1 != c2 && c1.name.equals(c2.name))
				{
					TypeChecker.report(3426, "Class " + c1.name + " duplicates " + c2.name, c1.name.getLocation());
				}
			}

			if (c1 instanceof TCSystemDefinition)
			{
				if (hasSystem)
				{
					TypeChecker.report(3294, "Only one system class permitted", c1.location);
				}
				else
				{
					hasSystem = true;
				}
			}
		}

		Environment allClasses = new PublicClassEnvironment(classes);

		for (TCClassDefinition c: classes)
		{
			c.implicitDefinitions(allClasses);
		}

    	for (TCClassDefinition c: classes)
		{
			try
			{
				Environment self = new PrivateClassEnvironment(c, allClasses);
				c.typeResolve(self);
			}
			catch (TypeCheckException te)
			{
				report(3427, te.getMessage(), te.location);

				if (te.extras != null)
				{
					for (TypeCheckException e: te.extras)
					{
						report(3427, e.getMessage(), e.location);
					}
				}
			}
		}

		for (TCClassDefinition c: classes)
		{
			c.checkOver();
		}

	    for (Pass pass: Pass.values())
		{
        	for (TCClassDefinition c: classes)
    		{
				try
				{
					Environment self = new PrivateClassEnvironment(c, allClasses);
	         		c.typeCheckPass(pass, self);
				}
				catch (TypeCheckException te)
				{
					report(3428, te.getMessage(), te.location);

    				if (te.extras != null)
    				{
    					for (TypeCheckException e: te.extras)
    					{
    						report(3428, e.getMessage(), e.location);
    					}
    				}
				}
    		}
		}
	    
	    TCDefinitionList allDefs = new TCDefinitionList();

    	for (TCClassDefinition c: classes)
		{
			c.initializedCheck();
			c.unusedCheck();
	    	allDefs.addAll(c.getDefinitions());
		}
    	
    	cyclicDependencyCheck(allDefs);
	}
}
