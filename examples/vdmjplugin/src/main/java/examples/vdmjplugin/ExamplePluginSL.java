/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package examples.vdmjplugin;

import static com.fujitsu.vdmj.plugins.PluginConsole.info;
import static com.fujitsu.vdmj.plugins.PluginConsole.infoln;
import static com.fujitsu.vdmj.plugins.PluginConsole.plural;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitOperationDefinition;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.analyses.ASTPlugin;

/**
 * Example VDMJ plugin
 */
public class ExamplePluginSL extends ExamplePlugin
{
	@SuppressWarnings("unchecked")
	@Override
	protected <T> T checkDefinitions()
	{
		ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
		ASTModuleList astModuleList = ast.getAST();
		List<VDMError> errors = new Vector<VDMError>();
		List<VDMWarning> warnings = new Vector<VDMWarning>();
		long before = System.currentTimeMillis();
		int count = 0;
		
		for (ASTModule module: astModuleList)
		{
			for (ASTDefinition def: module.defs)
			{
				count++;

				if (def instanceof ASTExplicitFunctionDefinition ||
					def instanceof ASTImplicitFunctionDefinition ||
					def instanceof ASTExplicitOperationDefinition ||
					def instanceof ASTImplicitOperationDefinition)
				{
					String correct = InitialUpper(def.name);
					
					if (!correct.equals(def.name.name))
					{
						errors.add(new VDMError(9999, "Name should be '" + correct + "'", def.name.location));
					}
					
					if (def.name.name.length() > maxLength)
					{
						warnings.add(new VDMWarning(9999, "Name should be " + maxLength + " chars or less", def.name.location));
					}
				}
			}
		}
		
		long after = System.currentTimeMillis();
		float duration = after - before;
		
		for (VDMMessage m: errors)
		{
			println(m.toString());
		}
		
		for (VDMMessage m: warnings)
		{
			println(m.toString());
		}
		
   		info("Style checked " + plural(count, "definition", "s") + " in " +
   			(double)(duration)/1000 + " secs. ");
   		info(errors.isEmpty() ? "No style errors" :
   			"Found " + plural(errors.size(), "style error", "s"));
  		infoln(warnings.isEmpty() ? "" : " and " + plural(warnings.size(), "warning", "s"));
		
		return (T) errors;	// Fatal errors returned
	}
}
