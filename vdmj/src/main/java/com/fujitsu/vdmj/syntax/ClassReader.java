/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.syntax;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.annotations.ASTAnnotationList;
import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.ast.definitions.ASTSystemDefinition;
import com.fujitsu.vdmj.ast.lex.LexCommentList;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.LocatedException;
import com.fujitsu.vdmj.messages.VDMError;

/**
 * A syntax analyser to parse class definitions.
 */

public class ClassReader extends SyntaxReader
{
	public ClassReader(LexTokenReader reader)
	{
		super(reader);
	}

	public ASTClassList readClasses()
	{
		ASTClassList list = new ASTClassList();

		try
		{
			if (lastToken().is(Token.EOF))
			{
				return list;	// The file is empty
			}

    		if (lastToken().isNot(Token.CLASS) && lastToken().isNot(Token.SYSTEM))
    		{
    			warning(5015,
    				"LaTeX source should start with %comment, \\document, \\section or \\subsection",
    				lastToken().location);

    			throwMessage(2005,
    				Settings.dialect == Dialect.VDM_RT ?
    					"Expecting list of 'class' or 'system' definitions" :
    					"Expecting list of 'class' definitions");
    		}

    		while (lastToken().is(Token.CLASS) || lastToken().is(Token.SYSTEM))
    		{
				LexCommentList comments = getComments();
				ASTAnnotationList annotations = readAnnotations(comments);
				annotations.astBefore(this);
				ASTClassDefinition clazz = null;

				if (lastToken().is(Token.CLASS))
    			{
					clazz = readClass();
    			}
    			else
    			{
    				clazz = readSystem();
    			}

				list.add(clazz);
				annotations.astAfter(this, clazz);
				clazz.setAnnotations(annotations);
				clazz.setComments(comments);
    		}
    		
    		trailingAnnotationCheck();

    		if (lastToken().isNot(Token.EOF))
    		{
    			throwMessage(2006, "Found tokens after class definitions");
    		}
		}
		catch (LocatedException e)
		{
			Token[] end = new Token[0];
			report(e, end, end);
		}

		return list;
	}

	private ASTClassDefinition readClass() throws ParserException, LexException
	{
		LexNameList superclasses = new LexNameList();

		if (lastToken().is(Token.CLASS))
		{
			setCurrentModule("");
			nextToken();
			LexIdentifierToken classId = readIdToken("Expecting class ID");
			LexNameToken className = classId.getClassName();
			setCurrentModule(classId.name);

			if (lastToken().is(Token.IS))
			{
				nextToken();
				checkFor(Token.SUBCLASS, 2075, "Expecting 'is subclass of'");
				checkFor(Token.OF, 2076, "Expecting 'is subclass of'");

				LexIdentifierToken id = readIdToken("Expecting class identifier");
				superclasses.add(id.getClassName());

				while (ignore(Token.COMMA))
				{
					id = readIdToken("Expecting class identifier");
					superclasses.add(id.getClassName());
				}
			}

			ASTDefinitionList members = getDefinitionReader().readDefinitions();
			checkFor(Token.END, 2077, "Expecting 'end' after class members");

			LexIdentifierToken endname =
				readIdToken("Expecting 'end <name>' after class members");

			if (classId != null && !classId.equals(endname))
			{
				throwMessage(2007, "Expecting 'end " + classId.name + "'");
			}

			LexLocation.addSpan(className, lastToken());
			return new ASTClassDefinition(className, superclasses, members);
		}
		else
		{
			throwMessage(2008, "Class does not start with 'class'");
		}

		return null;
	}

	private ASTSystemDefinition readSystem() throws ParserException, LexException
	{
		if (lastToken().is(Token.SYSTEM))
		{
			setCurrentModule("");
			nextToken();
			LexIdentifierToken classId = readIdToken("Expecting class ID");
			LexNameToken className = classId.getClassName();
			setCurrentModule(classId.name);

			if (lastToken().is(Token.IS))
			{
				nextToken();
				checkFor(Token.SUBCLASS, 2075, "Expecting 'is subclass of'");
				checkFor(Token.OF, 2076, "Expecting 'is subclass of'");

				throwMessage(2280, "System class cannot be a subclass");
			}

			ASTDefinitionList members = new ASTDefinitionList();
			DefinitionReader dr = getDefinitionReader();

    		while (lastToken().is(Token.INSTANCE) || lastToken().is(Token.OPERATIONS))
    		{
    			if (lastToken().is(Token.INSTANCE))
    			{
    				members.addAll(dr.readInstanceVariables());
    			}
    			else
    			{
    				members.addAll(dr.readOperations());
    			}
    		}

    		switch (lastToken().type)
    		{
    			case TYPES:
    			case VALUES:
    			case FUNCTIONS:
    			case THREAD:
    			case SYNC:
    				throwMessage(2290,
    					"System class can only define instance variables and a constructor");
    				break;

    			case END:
    				nextToken();
    				break;

    			default:
    				throwMessage(2077, "Expecting 'end' after system members");
    		}

			LexIdentifierToken endname =
				readIdToken("Expecting 'end <name>' after system members");

			if (classId != null && !classId.equals(endname))
			{
				throwMessage(2007, "Expecting 'end " + classId.name + "'");
			}

			LexLocation.addSpan(className, lastToken());
			return new ASTSystemDefinition(className, members);
		}
		else
		{
			throwMessage(2008, "System class does not start with 'system'");
		}

		return null;
	}
	
	@Override
	public List<VDMError> getErrors()
	{
		List<VDMError> errs = new Vector<VDMError>(reader.getErrors());
		errs.addAll(super.getErrors());
		return errs;
	}
	
	@Override
	public int getErrorCount()
	{
		return reader.getErrorCount() + super.getErrorCount();
	}
}
