/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.ModuleEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;

public class TCWarningAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;
	private int warningCount = 0;
	private String moduleName = null;
	private Set<Long> suppressed;
	
	public TCWarningAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void tcBefore(TCModule module, ModuleEnvironment e)
	{
		moduleName = module.name.getName();
		preCheck();
	}

	@Override
	public void tcBefore(TCClassDefinition clazz)
	{
		moduleName = clazz.name.getName();
		preCheck();
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		warningCount = TypeChecker.getWarningCount();
		preCheck();
	}

	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		warningCount = TypeChecker.getWarningCount();
		preCheck();
	}

	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		warningCount = TypeChecker.getWarningCount();
		preCheck();
	}
	
	private void preCheck()
	{
		if (args.isEmpty())
		{
			name.report(6010, "@Warning must have one or more numeric arguments");
		}
		
		suppressed = new HashSet<Long>();
		
		for (TCExpression arg: args)
		{
			if (!(arg instanceof TCIntegerLiteralExpression))
			{
				arg.report(6010, "@Warning arguments must be warning number literals");
			}
			else
			{
				TCIntegerLiteralExpression w = (TCIntegerLiteralExpression)arg;
				suppressed.add(w.value.value);
			}
		}
	}
	
	@Override
	public void tcAfter(TCClassDefinition clazz)
	{
		postCheck();
	}
	
	@Override
	public void tcAfter(TCModule module)
	{
		postCheck();
	}
	
	@Override
	public void tcAfter(TCModule module, ModuleEnvironment e)
	{
		postCheck();
	}
	
	@Override
	public void tcAfter(TCExpression exp, TCType type, Environment env, NameScope scope)
	{
		postCheck();
	}

	@Override
	public void tcAfter(TCStatement stmt, TCType type, Environment env, NameScope scope)
	{
		postCheck();
	}
	
	@Override
	public void tcAfter(TCDefinition def, TCType type, Environment env, NameScope scope)
	{
		postCheck();
	}
	
	private void postCheck()
	{
		Iterator<VDMWarning> witer = TypeChecker.getWarnings().iterator();
		
		if (moduleName == null)
		{
			for (int i=0; i < warningCount; i++)
			{
				witer.next();	// skip previous warnings
			}
		}
		
		while (witer.hasNext())
		{
			VDMWarning w = witer.next();
			
			if (moduleName != null && !w.location.module.equals(moduleName))
			{
				continue;
			}
			
			if (suppressed.contains((long)w.number))
			{
				witer.remove();
			}
		}
	}
	
	/**
	 * We have to use a doClose because the final stage of unusedChecks in the
	 * module typechecker are called after all the modules have been processed,
	 * so warnings may be raised after all of the tcAfter cases above.
	 */
	@Override
	public void doClose()
	{
		Iterator<VDMWarning> witer = TypeChecker.getWarnings().iterator();
		int myLine  = name.getLocation().startLine;
		File myFile = name.getLocation().file;
		
		while (witer.hasNext())
		{
			VDMWarning w = witer.next();
			
			if (moduleName != null && !w.location.module.equals(moduleName))
			{
				continue;
			}
			
			if (w.location.startLine == myLine + 1 &&
				w.location.file.equals(myFile) &&
				suppressed.contains((long)w.number))
			{
				// Warning is on the line after the one we annotated, so remove it
				witer.remove();
			}
		}
	}
}
