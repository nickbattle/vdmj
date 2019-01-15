/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
 *
 *	Author: Nick Battle
 *
 *	This file is part of Overture
 *
 ******************************************************************************/

package annotations.tc;

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
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTestAnnotation extends TCAnnotation
{
	public TCTestAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}
	
	public static void doInit()
	{
		System.out.printf("doInit TCTestAnnotation\n");
	}
	

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		System.out.printf("tcBefore %s %s\n", def.getClass().getSimpleName(), this);
	}
	
	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		System.out.printf("tcBefore %s %s\n", stmt.getClass().getSimpleName(), this);
	}
	
	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		System.out.printf("tcBefore %s %s\n", exp.getClass().getSimpleName(), this);
	}

	@Override
	public void tcBefore(TCModule m)
	{
		System.out.printf("tcBefore %s %s\n", m.getClass().getSimpleName(), this);
	}

	@Override
	public void tcBefore(TCClassDefinition clazz)
	{
		System.out.printf("tcBefore %s %s\n", clazz.getClass().getSimpleName(), this);
	}

	@Override
	public void tcAfter(TCDefinition def, TCType type, Environment env, NameScope scope)
	{
		System.out.printf("tcAfter %s %s\n", def.getClass().getSimpleName(), this);
	}
	
	@Override
	public void tcAfter(TCStatement stmt, TCType type, Environment env, NameScope scope)
	{
		System.out.printf("tcAfter %s %s\n", stmt.getClass().getSimpleName(), this);
	}
	
	@Override
	public void tcAfter(TCExpression exp, TCType type, Environment env, NameScope scope)
	{
		System.out.printf("tcAfter %s %s\n", exp.getClass().getSimpleName(), this);
	}

	@Override
	public void tcAfter(TCModule m)
	{
		System.out.printf("tcAfter %s %s\n", m.getClass().getSimpleName(), this);
	}

	@Override
	public void tcAfter(TCClassDefinition clazz)
	{
		System.out.printf("tcAfter %s %s\n", clazz.getClass().getSimpleName(), this);
	}
}
