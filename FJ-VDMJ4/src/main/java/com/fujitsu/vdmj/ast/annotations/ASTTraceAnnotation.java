package com.fujitsu.vdmj.ast.annotations;

import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;

public class ASTTraceAnnotation extends ASTAnnotation
{
	public ASTTraceAnnotation(LexIdentifierToken name, ASTExpressionList args)
	{
		super(name, args);
	}
}
