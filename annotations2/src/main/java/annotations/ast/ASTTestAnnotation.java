/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
 *
 *	Author: Nick Battle
 *
 *	This file is part of Overture
 *
 ******************************************************************************/

package annotations.ast;

import com.fujitsu.vdmj.ast.annotations.ASTAnnotation;
import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.syntax.DefinitionReader;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.syntax.StatementReader;

public class ASTTestAnnotation extends ASTAnnotation
{
	private static final long serialVersionUID = 1L;

	public ASTTestAnnotation(LexIdentifierToken name)
	{
		super(name);
	}
	
	public static void doInit()
	{
		System.out.printf("doInit ASTTestAnnotation\n");
	}
	
	@Override
	public void astBefore(DefinitionReader reader)
	{
		System.out.printf("astBefore %s %s\n", reader.getClass().getSimpleName(), this);
	}

	@Override
	public void astBefore(StatementReader reader)
	{
		System.out.printf("astBefore %s %s\n", reader.getClass().getSimpleName(), this);
	}

	@Override
	public void astBefore(ExpressionReader reader)
	{
		System.out.printf("astBefore %s %s\n", reader.getClass().getSimpleName(), this);
	}

	@Override
	public void astBefore(ModuleReader reader)
	{
		System.out.printf("astBefore %s %s\n", reader.getClass().getSimpleName(), this);
	}

	@Override
	public void astBefore(ClassReader reader)
	{
		System.out.printf("astBefore %s %s\n", reader.getClass().getSimpleName(), this);
	}


	@Override
	public void astAfter(DefinitionReader reader, ASTDefinition def)
	{
		System.out.printf("astAfter %s\n", reader.getClass().getSimpleName(), def.getClass().getSimpleName(), this);
	}

	@Override
	public void astAfter(StatementReader reader, ASTStatement stmt)
	{
		System.out.printf("astAfter %s %s %s\n", reader.getClass().getSimpleName(), stmt.getClass().getSimpleName(), this);
	}

	@Override
	public void astAfter(ExpressionReader reader, ASTExpression exp)
	{
		System.out.printf("astAfter %s %s %s\n", reader.getClass().getSimpleName(), exp.getClass().getSimpleName(), this);
	}

	@Override
	public void astAfter(ModuleReader reader, ASTModule module)
	{
		System.out.printf("astAfter %s %s %s\n", reader.getClass().getSimpleName(), module.getClass().getSimpleName(), this);
	}

	@Override
	public void astAfter(ClassReader reader, ASTClassDefinition clazz)
	{
		System.out.printf("astAfter %s %s %s\n", reader.getClass().getSimpleName(), clazz.getClass().getSimpleName(), this);
	}
}
