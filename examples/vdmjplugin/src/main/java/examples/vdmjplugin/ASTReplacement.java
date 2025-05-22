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

import java.io.File;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.ast.definitions.ASTValueDefinition;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.expressions.ASTStringLiteralExpression;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexStringToken;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.patterns.ASTIdentifierPattern;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.plugins.analyses.ASTPlugin;
import com.fujitsu.vdmj.plugins.analyses.ASTPluginSL;
import com.fujitsu.vdmj.typechecker.NameScope;

public class ASTReplacement extends ASTPluginSL
{
	public static ASTPlugin factory(Dialect dialect) throws Exception
	{
		if (dialect == Dialect.VDM_SL)
		{
			return new ASTReplacement();
		}
		else
		{
			throw new IllegalArgumentException("Only supported for VDM-SL");
		}
	}
	
	@Override
	protected List<VDMMessage> syntaxCheck()
	{
		// Write out a ValueDefinition for each source file :-)
		
		int n = 0;
		
		for (File file: files)
		{
			ASTDefinitionList definitions = new ASTDefinitionList();
			LexNameToken defname = new LexNameToken("DEFAULT", "SOURCE_" + ++n, LexLocation.ANY);
			ASTPattern pattern = new ASTIdentifierPattern(defname);
			LexStringToken string = new LexStringToken(file.getAbsolutePath(), LexLocation.ANY);
			ASTExpression expression = new ASTStringLiteralExpression(string);
			ASTValueDefinition def = new ASTValueDefinition(NameScope.GLOBAL, pattern, null, expression);
			definitions.add(def);
			ASTModule module = new ASTModule(file, definitions);
			astModuleList.add(module);
		}
		
		VDMMessage warning = new VDMWarning(9999, "ASTPlugin replacement is active!", LexLocation.ANY);
		List<VDMMessage> result = new Vector<VDMMessage>();
		result.add(warning);
		
		return result;
	}
}
