/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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

package annotations.tc;

import java.util.Stack;

import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.ModuleEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.PrivateClassEnvironment;

public class TCDocLinkAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;
	
	private static Stack<TCDocLinkAnnotation> stack = new Stack<TCDocLinkAnnotation>();

	public TCDocLinkAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void tcBefore(TCModule module, ModuleEnvironment env)
	{
		stack.clear();
		stack.push(this);
		check();
	}

	@Override
	public void tcBefore(TCClassDefinition clazz, PrivateClassEnvironment env)
	{
		stack.clear();
		stack.push(this);
		check();
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		stack.push(this);
		check();
	}

	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		stack.push(this);
		check();
	}
	
	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		stack.push(this);
		check();
	}
	
	@Override
	public void tcAfter(TCClassDefinition m, PrivateClassEnvironment env)
	{
		stack.pop();
	}

	@Override
	public void tcAfter(TCModule m, ModuleEnvironment e)
	{
		stack.pop();
	}

	@Override
	public void tcAfter(TCDefinition def, TCType type, Environment env, NameScope scope)
	{
		stack.pop();
	}
	
	@Override
	public void tcAfter(TCExpression exp, TCType type, Environment env, NameScope scope)
	{
		stack.pop();
	}

	@Override
	public void tcAfter(TCStatement stmt, TCType type, Environment env, NameScope scope)
	{
		stack.pop();
	}

	public void check()
	{
		if (args.isEmpty())
		{
			name.report(6008, "@DocLink(Expression...)");
		}
	}
	
	public static Stack<TCDocLinkAnnotation> enclosing()
	{
		return stack;
	}
}
