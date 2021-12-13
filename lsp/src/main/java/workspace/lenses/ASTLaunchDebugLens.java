/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package workspace.lenses;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitOperationDefinition;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.Token;

import json.JSONArray;

public class ASTLaunchDebugLens extends AbstractLaunchDebugLens
{
	private final String CODE_LENS_COMMAND = "vdm-vscode.addLensRunConfigurationWarning";

	@Override
	public <DEF> JSONArray getDefinitionLenses(DEF definition)
	{
		ASTDefinition def = (ASTDefinition)definition;	// Better be!
		JSONArray results = new JSONArray();
		
		if ("vscode".equals(getClientName()) && isPublic(def))
		{
			boolean included = false;
			
			if (def instanceof ASTExplicitFunctionDefinition)
			{
				included = true;
			}
			else if (def instanceof ASTImplicitFunctionDefinition)
			{
				ASTImplicitFunctionDefinition imdef = (ASTImplicitFunctionDefinition) def;
				included = (imdef.body != null);
			}
			else if (def instanceof ASTExplicitOperationDefinition)
			{
				ASTExplicitOperationDefinition exop = (ASTExplicitOperationDefinition) def;
				String applyName = exop.name.getName();
				String defaultName = exop.name.module;
				included = (!applyName.equals(defaultName));		// Not a constructor
			}
			else if (def instanceof ASTImplicitOperationDefinition)
			{
				ASTImplicitOperationDefinition imop = (ASTImplicitOperationDefinition) def;
				String applyName = imop.name.getName();
				String defaultName = imop.name.module;
				included = (!applyName.equals(defaultName) && imop.body != null);	// Not a constructor
			}
			
			if (included)
			{
				results.add(makeLens(def.location, "Launch", CODE_LENS_COMMAND));
				results.add(makeLens(def.location, "Debug", CODE_LENS_COMMAND));
			}
		}
		
		return results;
	}
		
	private boolean isPublic(ASTDefinition def)
	{
		if (Settings.dialect != Dialect.VDM_SL)
		{
			return def.accessSpecifier.access == Token.PUBLIC;	// Not private or protected
		}
		else
		{
			return true;
		}
	}
}
