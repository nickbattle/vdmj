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

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.tc.TCRecursiveLoops;
import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

/**
 * A class to coordinate all module type checking processing.
 */

public class ModuleTypeChecker extends TypeChecker
{
	/** The list of executableModules to check. */
	private final TCModuleList modules;

	/**
	 * Create a type checker with the list of executableModules passed. The warnings
	 * flag indicates whether warnings should be printed or just counted.
	 *
	 * @param modules
	 */

	public ModuleTypeChecker(TCModuleList modules)
	{
		super();
		this.modules = modules;
	}

	/**
	 * Perform the type checking for the set of executableModules. This is a complicated
	 * process.
	 * p
	 * First the module names are checked for uniqueness. Then each module
	 * generates its implicit definitions (eg. for pre_ and post_ functions).
	 * Then export definitions for each module are generated, and the import
	 * definitions linked to them. Next all the definition in the set of
	 * executableModules are type resolved by creating a list of all executableModules' definitions
	 * and calling their typeResolve methods. Then the type checking of the
	 * executableModules' definitions can proceed, covering the types, values and
	 * remaining definitions in that order. Next, the declared types of the
	 * imports for each module are compared with the (now determined) types of
	 * the exports. Finally the usage of the imports and definitions for each
	 * module are checked.
	 */

	@Override
	public void typeCheck()
	{
		// Check for module name duplication

		boolean hasFlat = false;

		for (TCModule m1: modules)
		{
			for (TCModule m2: modules)
			{
				if (m1 != m2 && m1.name.equals(m2.name))
				{
					TypeChecker.report(3429, "Module " + m1.name + " duplicates " + m2.name, m1.name.getLocation());
				}
			}

			if (m1.isFlat)
			{
				hasFlat = true;
			}
			else
			{
				if (hasFlat && Settings.release == Release.CLASSIC)
				{
					TypeChecker.report(3308, "Cannot mix executableModules and flat specifications", m1.name.getLocation());
				}
			}
		}

		// Generate implicit definitions for pre_, post_, inv_ functions etc.

		for (TCModule m: modules)
		{
			Environment env = new ModuleEnvironment(m);
			m.defs.implicitDefinitions(env);
		}

		// Exports have to be identified before imports can be processed.

		for (TCModule m: modules)
		{
			m.processExports();			// Populate exportDefs
		}

		// Process the imports early because renamed imports create definitions
		// which can affect type resolution.

		for (TCModule m: modules)
		{
			m.processImports(modules);	// Populate importDefs
		}

		// Create a list of all definitions from all executableModules, including
		// imports of renamed definitions. We put the imports ahead of the
		// other definitions so that they are found first.

		TCDefinitionList alldefs = new TCDefinitionList();

		for (TCModule m: modules)
		{
			for (TCDefinition d: m.importdefs)
			{
				alldefs.add(d);
			}
		}

		for (TCModule m: modules)
		{
			for (TCDefinition d: m.defs)
			{
				alldefs.add(d);
			}
		}

		for (TCModule m: modules)
		{
			m.checkOver();
		}

		// Attempt type resolution of definitions from all modules. We have to resolve module types
		// against all possible types (rather than per module) because the resolution process chases
		// the full tree of subtypes, which may cross multiple modules. Subsequently, the typecheck
		// process verifies that the types used within a module are imported, if necessary.

		Environment allenv = new FlatCheckedEnvironment(alldefs, NameScope.NAMESANDSTATE);

		for (TCDefinition d: alldefs)
		{
			try
			{
				d.typeResolve(allenv);
			}
			catch (TypeCheckException te)
			{
				report(3430, te.getMessage(), te.location);
				
				if (te.extras != null)
				{
					for (TypeCheckException e: te.extras)
					{
						report(3430, e.getMessage(), e.location);
					}
				}
			}
		}
		
		// Initialise any annotations
		TCAnnotation.init(new FlatCheckedEnvironment(alldefs, NameScope.NAMESANDSTATE));

		for (TCModule m: modules)
		{
			if (m.annotations != null) m.annotations.tcBefore(m);
		}
		
		// Prepare to look for recursive loops
		TCRecursiveLoops.getInstance().reset();

		// Proceed to type check all definitions, considering types, values
		// and remaining definitions, in that order.

		for (Pass pass: Pass.values())
		{
			for (TCModule m: modules)
			{
				TypeComparator.setCurrentModule(m.name.getName());
				ModuleEnvironment e = new ModuleEnvironment(m);

				if (pass == Pass.DEFS && m.annotations != null)
				{
					m.annotations.tcBefore(m, e);
				}

				for (TCDefinition d: m.defs)
				{
					if (d.pass == pass)
					{
						try
						{
							d.typeCheck(e, NameScope.NAMES);
						}
						catch (TypeCheckException te)
						{
							report(3430, te.getMessage(), te.location);
							
		    				if (te.extras != null)
		    				{
		    					for (TypeCheckException ex: te.extras)
		    					{
		    						report(3430, ex.getMessage(), ex.location);
		    					}
		    				}
						}
					}
				}
				
				if (pass == Pass.DEFS && m.annotations != null)
				{
					m.annotations.tcAfter(m, e);
				}
			}
			
			// After the VALUES pass, ValueDefinitions will have replaced their TCUntypedDefinition "defs"
			// with typed TCLocalDefinitions, so we refresh the export/importDefs to allow later passes
			// to see the correct types of imported definitions.
			
			if (pass == Pass.VALUES)
			{
				for (TCModule m: modules)
				{
					m.processExports();				// Re-populate exports
				}
				
				for (TCModule m: modules)
				{
					m.processImports(modules);		// Re-populate importDefs
				}
			}
		}
		
		// Prepare to look for recursive loops
		TCRecursiveLoops.getInstance().typeCheck(modules);

		// Report any discrepancies between the final checked types of
		// definitions and their explicit imported types.

		for (TCModule m: modules)
		{
			m.processExports();				// Re-populate exports again
		}
		
		for (TCModule m: modules)
		{
			m.processImports(modules);		// Re-populate importDefs again

			try
			{
				m.typeCheckImports();		// Imports compared to exports
				m.typeCheckExports();		// Exports compared to definitions
			}
			catch (TypeCheckException te)
			{
				report(3432, te.getMessage(), te.location);
			}
		}

		// Any names that have not been referenced or exported produce "unused"
		// warnings.

    	for (TCModule m: modules)
		{
			m.importdefs.unusedCheck();
			m.defs.unusedCheck();
		}

    	// Post process annotations
		for (TCModule m: modules)
		{
			if (m.annotations != null) m.annotations.tcAfter(m);
		}

		// Close any annotations
		TCAnnotation.close();

    	// Check for inter-definition cyclic dependencies before initialization
    	cyclicDependencyCheck(alldefs);

		// Calculate the transitive update sets for the operations
		// populateTransitiveUpdates(alldefs);
	}
}
