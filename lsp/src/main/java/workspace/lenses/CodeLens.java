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

import java.io.File;

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import json.JSONArray;
import json.JSONObject;
import lsp.Utils;

/**
 * The base class for all code lenses.
 */
abstract public class CodeLens
{
	abstract public JSONArray codeLenses(ASTDefinition definition, File file);
	abstract public JSONArray codeLenses(TCDefinition definition, File file);
	
	protected JSONObject makeLens(LexLocation location, String title, String command)
	{
		return new JSONObject(
				"range", Utils.lexLocationToRange(location),
				"command", new JSONObject("title", title, "command", command));
	}
	
	protected JSONObject makeLens(LexLocation location, String title, String command, JSONArray arguments)
	{
		JSONObject lens = makeLens(location, title, command);
		JSONObject cmd = lens.get("command");
		cmd.put("command", new JSONObject("title", title, "command", command, "arguments", arguments));
		return lens;
	}
}
