package com.fujitsu.vdmj.tc.annotations;

import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTraceAnnotation extends TCAnnotation
{
	public TCTraceAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void typeCheck(Environment env, NameScope scope)
	{
		for (TCExpression arg: args)
		{
			if (!(arg instanceof TCVariableExpression))
			{
				arg.report(3358, "@Trace argument must be an identifier");
			}
			else
			{
				arg.typeCheck(env, null, scope, null);	// Just checks scope
			}
		}
	}
}
