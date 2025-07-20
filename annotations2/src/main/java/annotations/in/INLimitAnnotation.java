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

import com.fujitsu.vdmj.in.annotations.INAnnotatedExpression;
import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INIntegerLiteralExpression;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;

public class INLimitAnnotation extends INAnnotation
{
	private static final long serialVersionUID = 1L;

	public INLimitAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
	}
	
	@Override
	public void inAfter(INExpression node, Value value, Context ctxt)
	{
		INAnnotatedExpression aexp = (INAnnotatedExpression) node;
		INIntegerLiteralExpression limit = (INIntegerLiteralExpression) args.get(0);
		long count = value.apply(new CountVisitor(), null);
		
		if (count > limit.value.value)
		{
			String msg = String.format("@Limit Value count %d has exceeded the limit of %d", count, limit.value.value);
			throw new ContextException(8206, msg, aexp.expression.location, ctxt);
		}
	}
}
