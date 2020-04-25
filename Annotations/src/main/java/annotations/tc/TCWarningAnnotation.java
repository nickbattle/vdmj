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
 *
 ******************************************************************************/

package annotations.tc;

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
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;

public class TCWarningAnnotation extends TCAnnotation
{
	private int warningCount = 0;
	private Set<Long> suppressed;
	
	public TCWarningAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		preCheck();
	}

	@Override
	public void tcBefore(TCModule module)
	{
		preCheck();
	}

	@Override
	public void tcBefore(TCClassDefinition clazz)
	{
		preCheck();
	}

	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		preCheck();
	}

	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		preCheck();
	}
	
	private void preCheck()
	{
		if (args.isEmpty())
		{
			name.report(6007, "@Warning must have one or more numeric arguments");
		}
		
		warningCount = TypeChecker.getWarningCount();
		suppressed = new HashSet<Long>();
		
		for (TCExpression arg: args)
		{
			if (!(arg instanceof TCIntegerLiteralExpression))
			{
				arg.report(6007, "@Warning arguments must be warning number literals");
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
		
		for (int i=0; i < warningCount; i++)
		{
			witer.next();	// skip previous warnings
		}
		
		while (witer.hasNext())
		{
			VDMWarning w = witer.next();
			
			if (suppressed.contains((long)w.number))
			{
				witer.remove();
			}
		}
	}
	
	@Override
	public void doClose()
	{
		Iterator<VDMWarning> witer = TypeChecker.getWarnings().iterator();
		int startLine = name.getLocation().startLine;
		
		while (witer.hasNext())
		{
			VDMWarning w = witer.next();
			
			if (w.location.startLine == startLine + 1 && suppressed.contains((long)w.number))
			{
				// Warning is on the line after the one we annotated, so remove it
				witer.remove();
			}
		}
	}
}
