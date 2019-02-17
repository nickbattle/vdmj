/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
 *
 *	Author: Nick Battle
 *
 *	This file is part of Overture
 *
 ******************************************************************************/

package annotations.in;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;

public class INTestAnnotation extends INAnnotation
{
	public INTestAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
	}
	
	public static void doInit()
	{
		System.out.printf("doInit INTestAnnotation\n");
	}
	
	@Override
	public void inBefore(INStatement node, Context ctxt)
	{
		System.out.printf("inBefore %s %s\n", node.getClass().getSimpleName(), this);
	}

	@Override
	public void inAfter(INStatement node, Value value, Context ctxt)
	{
		System.out.printf("inAfter %s %s\n", node.getClass().getSimpleName(), this);
	}

	@Override
	public void inBefore(INExpression node, Context ctxt)
	{
		System.out.printf("inBefore %s %s\n", node.getClass().getSimpleName(), this);
	}

	@Override
	public void inAfter(INExpression node, Value value, Context ctxt)
	{
		System.out.printf("inAfter %s %s\n", node.getClass().getSimpleName(), this);
	}
}
