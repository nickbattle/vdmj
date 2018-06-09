package com.fujitsu.vdmj.po.annotations;

import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

public class POTraceAnnotation extends POAnnotation
{
	public POTraceAnnotation(TCIdentifierToken name, POExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void pog()
	{
		// Nothing to do for @Trace?
	}
}
