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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.syntax;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.annotations.ASTAnnotation;
import com.fujitsu.vdmj.ast.annotations.ASTAnnotationList;
import com.fujitsu.vdmj.ast.lex.LexCommentList;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.ConsoleWriter;
import com.fujitsu.vdmj.messages.LocatedException;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.util.GetResource;


/**
 * The parent class of all syntax readers.
 */
public abstract class SyntaxReader
{
	/** The lexical analyser. */
	protected final LexTokenReader reader;
	/** The dialect of VDM that we're parsing. */
	protected final Dialect dialect;

	/** A DefinitionReader, if created. */
	protected DefinitionReader definitionReader = null;
	/** An ExpressionReader, if created. */
	protected ExpressionReader expressionReader = null;
	/** A PatternReader, if created. */
	protected PatternReader patternReader = null;
	/** A TypeReader, if created. */
	protected TypeReader typeReader = null;
	/** A BindReader, if created. */
	protected BindReader bindReader = null;
	/** A StatementReader, if created. */
	protected StatementReader statementReader = null;
	/** A ClassReader, if created. */
	protected ClassReader classReader = null;

	/** The errors raised. */
	private List<VDMError> errors = new Vector<VDMError>();

	/** The warnings raised. */
	private List<VDMWarning> warnings = new Vector<VDMWarning>();

	/** The sub-readers defined, if any. */
	private List<SyntaxReader> readers = new Vector<SyntaxReader>();

	/** The maximum number of syntax errors allowed in one Reader. */
	private static final int MAX = 100;

	private static List<String> annotationClasses = null;

	/**
	 * Create a reader with the given lexical analyser and VDM++ flag.
	 */
	protected SyntaxReader(LexTokenReader reader)
	{
		this.reader = reader;
		this.dialect = reader.dialect;
		reader.setSyntaxReader(this);
	}

	/**
	 * Read the next token from the lexical analyser, and advance by
	 * one token.
	 *
	 * @return The next token.
	 */
	protected LexToken nextToken() throws LexException
	{
		return reader.nextToken();
	}

	/**
	 * Return the last token read by the lexical analyser without
	 * advancing. Repeated calls to this method will return the same
	 * result.
	 *
	 * @return The last token again.
	 */
	protected LexToken lastToken() throws LexException
	{
		return reader.getLast();
	}

	/**
	 * Return the last token read, and also advance by one token. This
	 * is equivalent to calling {@link #lastToken} followed by
	 * {@link #nextToken}, but returning the result of lastToken.
	 *
	 * @return The last token.
	 * @throws LexException
	 */
	protected LexToken readToken() throws LexException
	{
		LexToken tok = reader.getLast();
		reader.nextToken();
		return tok;
	}

	/**
	 * Set the name of the current module or class. Unqualified symbol names use
	 * this as their module/class name. See {@link #idToName}.
	 *
	 * @param module
	 */
	public void setCurrentModule(String module)
	{
		reader.currentModule = module;
	}

	/**
	 * @return The current module/class name.
	 */
	public String getCurrentModule()
	{
		return reader.currentModule;
	}

	/**
	 * Convert an identifier into a name. A name is an identifier that has
	 * a module name qualifier, so this method uses the current module to
	 * convert the identifier passed in.
	 *
	 * @param id The identifier to convert
	 * @return The corresponding name.
	 */
	protected LexNameToken idToName(LexIdentifierToken id)
	{
		LexNameToken name = new LexNameToken(reader.currentModule, id);
		return name;
	}

	/**
	 * Return the last token, converted to a {@link LexIdentifierToken}. If
	 * the last token is not an identifier token, an exception is thrown
	 * with the message passed in.
	 *
	 * @return	The last token as a LexIdentifierToken.
	 * @throws LexException
	 */
	protected LexIdentifierToken lastIdToken()
		throws ParserException, LexException
	{
		LexToken tok = reader.getLast();

		if (tok.type == Token.IDENTIFIER)
		{
			LexIdentifierToken id = (LexIdentifierToken)tok;
			
			if (id.old)
			{
				throwMessage(2295, "Can't use old name " + id + " here", tok);
			}
			
			return id;
		}

		throwMessage(2058, "Expecting Identifier");
		return null;
	}

	/**
	 * Return the last token, converted to a {@link LexNameToken}. If
	 * the last token is not a name token, or an identifier token that can
	 * be converted to a name, an exception is thrown with the message
	 * passed in.
	 *
	 * @return The last token as a LexIdentifierToken.
	 * @throws LexException
	 * @throws ParserException
	 */
	protected LexNameToken lastNameToken()
		throws LexException, ParserException
	{
		LexToken tok = reader.getLast();

		if (tok instanceof LexNameToken)
		{
			LexNameToken name = (LexNameToken)tok;
			
			if (name.old)
			{
				throwMessage(2295, "Can't use old name " + name + " here", tok);
			}
			
			return name;
		}
		else if (tok instanceof LexIdentifierToken)
		{
			LexIdentifierToken id = (LexIdentifierToken)tok;
			
			if (id.old)
			{
				throwMessage(2295, "Can't use old name " + id + " here", tok);
			}
			
			return new LexNameToken(reader.currentModule, id);
		}

		throwMessage(2059, "Expecting a name");
		return null;
	}

	/**
	 * Return the last token as an identifier, and advance by one token. This
	 * is similar to calling {@link #lastIdToken} followed by nextToken, and
	 * returning the result of the lastIdToken.
	 *
	 * @param message The message to throw if the last token is not an id.
	 * @return The last token as a LexIdentifierToken.
	 * @throws LexException
	 * @throws ParserException
	 */
	protected LexIdentifierToken readIdToken(String message)
			throws LexException, ParserException
	{
		return readIdToken(message, false);
	}
	
	protected LexIdentifierToken readIdToken(String message, boolean reservedOK)
		throws LexException, ParserException
	{
		LexToken tok = reader.getLast();

		if (tok.type == Token.IDENTIFIER)
		{
			nextToken();
			LexIdentifierToken id = (LexIdentifierToken)tok;
			
			if (id.old)
			{
				throwMessage(2295, "Can't use old name " + id + " here", tok);
			}
			
			if (!reservedOK && isReserved(id.name))
			{
				throwMessage(2295, "Name " + id + " contains a reserved prefix", tok);
			}
			
			return id;
		}

		if (tok.type == Token.NAME)
		{
			message = "Found qualified name " + tok + ". " + message;
		}

		throwMessage(2060, message);
		return null;
	}

	/**
	 * Return the last token as a name, and advance by one token. This
	 * is similar to calling {@link #lastNameToken} followed by nextToken, and
	 * returning the result of the lastNameToken.
	 *
	 * @param message The message to throw if the last token is not a name.
	 * @return The last token as a LexNameToken.
	 * @throws LexException
	 * @throws ParserException
	 */
	protected LexNameToken readNameToken(String message)
			throws LexException, ParserException
	{
		return readNameToken(message, false);
	}
	
	protected LexNameToken readNameToken(String message, boolean reservedOK)
		throws LexException, ParserException
	{
		LexToken tok = reader.getLast();
		nextToken();

		if (tok instanceof LexNameToken)
		{
			LexNameToken name = (LexNameToken)tok;
			
			if (name.old)
			{
				throwMessage(2295, "Can't use old name " + name + " here", tok);
			}
			
			if (isReserved(name.name))
			{
				throwMessage(2295, "Name " + name + " contains a reserved prefix", tok);
			}
			
			return name;
		}
		else if (tok instanceof LexIdentifierToken)
		{
			LexIdentifierToken id = (LexIdentifierToken)tok;
			
			if (id.old)
			{
				throwMessage(2295, "Can't use old name " + id + " here", tok);
			}
			
			if (!reservedOK && isReserved(id.name))
			{
				throwMessage(2295, "Name " + id + " contains a reserved prefix", tok);
			}
			
			return new LexNameToken(reader.currentModule, id);
		}

		throwMessage(2061, message);
		return null;
	}
	
	/**
	 * Read any annotations from the collected comments, and clear them. Note that we
	 * don't parse annotations while inside the annotation parser.
	 */
	private static int readingAnnotations = 0;

	protected ASTAnnotationList readAnnotations(LexCommentList comments) throws LexException, ParserException
	{
		ASTAnnotationList annotations = new ASTAnnotationList();

		if (!Settings.annotations || readingAnnotations > 0)
		{
			return annotations;		// ignore nested annotations
		}
		else
		{
			readingAnnotations++;
		}
		
		for (int i=0; i<comments.size(); i++)
		{
			if (comments.comment(i).trim().startsWith("@"))
			{
				try
				{
					annotations.add(readAnnotation(new LexTokenReader(
							comments.comment(i), comments.location(i), reader)));
				}
				catch (Exception e)
				{
					if (Properties.annotations_debug)
					{
						Console.err.println("Annotations: " + e.getMessage() + " at " + comments.location(i));
					}
				}
			}
		}
		
		readingAnnotations--;
		return annotations;
	}
	
	private ASTAnnotation readAnnotation(LexTokenReader ltr) throws LexException, ParserException
	{
		ltr.nextToken();
		
		if (ltr.nextToken().is(Token.IDENTIFIER))
		{
			LexIdentifierToken name = (LexIdentifierToken)ltr.getLast(); 
			ASTAnnotation annotation = loadAnnotation(name);
			annotation.parse(ltr);
			return annotation;
		}
		
		throw new LexException(0, "Comment doesn't start with @Annotation", LexLocation.ANY);
	}

	protected void trailingAnnotationCheck() throws LexException, ParserException
	{
		ASTAnnotationList trailing = readAnnotations(getComments());
		
		if (!trailing.isEmpty())
		{
			for (ASTAnnotation annotation: trailing)
			{
				warning(5038, "Trailing annotation ignored: " + annotation, annotation.name.location);
			}
		}
	}
	
	/**
	 * @return A new DefinitionReader.
	 */
	protected DefinitionReader getDefinitionReader()
	{
		if (definitionReader == null)
		{
			definitionReader = new DefinitionReader(reader);
			readers.add(definitionReader);
		}

		return definitionReader;
	}

	/**
	 * @return A new DefinitionReader.
	 */
	protected ExpressionReader getExpressionReader()
	{
		if (expressionReader == null)
		{
			expressionReader = new ExpressionReader(reader);
			readers.add(expressionReader);
		}

		return expressionReader;
	}

	/**
	 * @return A new PatternReader.
	 */
	protected PatternReader getPatternReader()
	{
		if (patternReader == null)
		{
			patternReader = new PatternReader(reader);
			readers.add(patternReader);
		}

		return patternReader;
	}

	/**
	 * @return A new TypeReader.
	 */
	protected TypeReader getTypeReader()
	{
		if (typeReader == null)
		{
			typeReader = new TypeReader(reader);
			readers.add(typeReader);
		}

		return typeReader;
	}

	/**
	 * @return A new BindReader.
	 */
	protected BindReader getBindReader()
	{
		if (bindReader == null)
		{
			bindReader = new BindReader(reader);
			readers.add(bindReader);
		}

		return bindReader;
	}

	/**
	 * @return A new StatementReader.
	 */
	protected StatementReader getStatementReader()
	{
		if (statementReader == null)
		{
			statementReader = new StatementReader(reader);
			readers.add(statementReader);
		}

		return statementReader;
	}

	/**
	 * @return A new ClassReader.
	 */
	protected ClassReader getClassReader()
	{
		if (classReader == null)
		{
			classReader = new ClassReader(reader);
			readers.add(classReader);
		}

		return classReader;
	}

	public void close()
	{
		reader.close();
	}

	/**
	 * If the last token is as expected, advance, else raise an error.
	 * @param tok The token type to check for.
	 * @param number The error number.
	 * @param message The error message to raise if the token is not as expected.
	 * @throws LexException
	 * @throws ParserException
	 */
	protected void checkFor(Token tok, int number, String message)
		throws LexException, ParserException
	{
		if (lastToken().is(tok))
		{
			nextToken();
		}
		else
		{
			throwMessage(number, message);
		}
	}

	/**
	 * If the last token is the one passed, advance by one, else do nothing.
	 *
	 * @param tok The token type to check for.
	 * @return True if the token was skipped.
	 * @throws LexException
	 */
	protected boolean ignore(Token tok) throws LexException
	{
		if (lastToken().is(tok))
		{
			nextToken();
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Raise a {@link ParserException} at the current location.
	 * @param number The error number.
	 * @param message The error message.
	 *
	 * @throws ParserException
	 * @throws LexException
	 */
	protected void throwMessage(int number, String message)
		throws ParserException, LexException
	{
		throw new ParserException(
			number, message, lastToken().location, reader.getTokensRead());
	}

	/**
	 * Raise a {@link ParserException} at the location of the token passed in.
	 * @param number The error number.
	 * @param message The error message.
	 * @param token The location of the error.
	 *
	 * @throws ParserException
	 */
	protected void throwMessage(int number, String message, LexToken token)
		throws ParserException
	{
		throw new ParserException(
			number, message, token.location, reader.getTokensRead());
	}
	
	/**
	 * Raise a {@link ParserException} with a given toek depth.
	 * @param number The error number.
	 * @param message The error message.
	 * @param token The location of the error.
	 * @param depth The depth of the exception (tokens read).
	 *
	 * @throws ParserException
	 */ 
	protected void throwMessage(int number, String message, int depth)
			throws ParserException, LexException
	{
		throw new ParserException(number, message, lastToken().location, depth);
	}
		
	/**
	 * Raise a syntax error and attempt to recover. The error is added to the errors
	 * list, and if this exceeds 100 errors the parser is aborted. The "after"
	 * and "upto" lists of token types are then used to control the advance of the
	 * parser to skip beyond the error. Tokens are read until one occurs in either
	 * list or EOF is reached. If the token is in the "after" list, one more token
	 * is read before returning; if it is in the "upto" list, the last token is
	 * left pointing to the token before returning. If EOF is reached, the method
	 * returns.
	 *
	 * @param error The exception that caused the error.
	 * @param after A list of tokens to recover to, and step one beyond.
	 * @param upto A list of tokens to recover to.
	 */
	protected void report(LocatedException error, Token[] after, Token[] upto)
	{
		if (errors.size() < MAX)
		{
			VDMError vdmerror = new VDMError(error);
			errors.add(vdmerror);
	
			if (errors.size() == MAX)
			{
				errors.add(new VDMError(9, "Too many syntax errors", error.location));
				// throw new InternalException(9, "Too many syntax errors");
			}
		}

		// Either leave one token beyond something in the after list, or
		// at something in the next upto list.

		List<Token> afterList = Arrays.asList(after);
		List<Token> uptoList = Arrays.asList(upto);

		try
		{
    		Token tok = lastToken().type;

    		while (!uptoList.contains(tok) && tok != Token.EOF)
    		{
    			if (afterList.contains(tok))
    			{
    				nextToken();
    				break;
    			}

    			tok = nextToken().type;
    		}
		}
		catch (LexException le)
		{
			report(le.number, le.getMessage(), le.location);
		}
	}

	/**
	 * Report a warning. Unlike errors, this does no token recovery.
	 */
	public void warning(int no, String msg, LexLocation location)
	{
		if (warnings.size() < MAX)
		{
			VDMWarning vdmwarning = new VDMWarning(no, msg, location);
			warnings.add(vdmwarning);
	
			if (warnings.size() == MAX)
			{
				warnings.add(new VDMWarning(9, "Too many warnings", location));
				// throw new InternalException(9, "Too many warnings");
			}
		}
	}

	/**
	 * Report an error. Unlike errors, this does no token recovery.
	 */
	public void report(int no, String msg, LexLocation location)
	{
		if (errors.size() < MAX)
		{
			VDMError vdmerror = new VDMError(no, msg, location);
			errors.add(vdmerror);
	
			if (errors.size() == MAX)
			{
				errors.add(new VDMError(9, "Too many syntax errors", location));
				// throw new InternalException(9, "Too many syntax errors");
			}
		}
	}

	/**
	 * @return The error count from all readers that can raise errors.
	 */
	public int getErrorCount()
	{
		int size = 0;

		for (SyntaxReader rdr: readers)
		{
			size += rdr.getErrorCount();
		}

		return size + errors.size();
	}

	/**
	 * @return The errors from all readers that can raise errors.
	 */
	public List<VDMError> getErrors()
	{
		List<VDMError> list = new Vector<VDMError>();

		for (SyntaxReader rdr: readers)
		{
			list.addAll(rdr.getErrors());
		}

		list.addAll(errors);
		return list;
	}

	/**
	 * @return The warning count from all readers that can raise warnings.
	 */
	public int getWarningCount()
	{
		int size = 0;

		for (SyntaxReader rdr: readers)
		{
			size += rdr.getWarningCount();
		}

		return size + warnings.size();
	}

	/**
	 * @return The warnings from all readers that can raise warnings.
	 */
	public List<VDMWarning> getWarnings()
	{
		List<VDMWarning> list = new Vector<VDMWarning>();

		for (SyntaxReader rdr: readers)
		{
			list.addAll(rdr.getWarnings());
		}

		list.addAll(warnings);
		return list;
	}

	/**
	 * Print errors and warnings to the PrintWriter passed.
	 */
	public void printErrors(ConsoleWriter out)
	{
		for (VDMError e: getErrors())
		{
			out.println(e.toString());
		}
	}

	public void printWarnings(ConsoleWriter out)
	{
		for (VDMWarning w: getWarnings())
		{
			out.println(w.toString());
		}
	}

	@Override
	public String toString()
	{
		return reader.toString();
	}

	protected ASTAnnotation loadAnnotation(LexIdentifierToken name)
		throws ParserException, LexException
	{
		String classpath = Properties.annotations_packages;
		String[] packages = classpath.split(";|:");
		String astName = "AST" + name + "Annotation";
		
		if (annotationClasses == null)
		{
			try
			{
				annotationClasses = GetResource.readResource("vdmj.annotations");
			}
			catch (Exception e)
			{
				// ignore
			}
		}
		
		/*
		 * The original method to load annotations uses the annotation_packages property.
		 */
		
		for (String pack: packages)
		{
			try
			{
				Class<?> clazz = Class.forName(pack + "." + astName);
				Constructor<?> ctor = clazz.getConstructor(LexIdentifierToken.class);
				return (ASTAnnotation) ctor.newInstance(name);
			}
			catch (ClassNotFoundException e)
			{
				// Try the next package
			}
			catch (Exception e)
			{
				throwMessage(2334, "Failed to instantiate " + astName);
			}
		}
		
		/*
		 * The preferred method of loading uses an "annotations" resource file, allowing
		 * annotations to be in any package, though the AST<name>Annotation rule remains.
		 */
		
		if (annotationClasses != null)
		{
			for (String annotationClass: annotationClasses)
			{
				try
				{
					if (annotationClass.endsWith("." + astName))
					{
						Class<?> clazz = Class.forName(annotationClass);
						Constructor<?> ctor = clazz.getConstructor(LexIdentifierToken.class);
						return (ASTAnnotation) ctor.newInstance(name);
					}
				}
				catch (Exception e)
				{
					throwMessage(2334, "Failed to instantiate " + astName);
				}
			}
		}

		throwMessage(2334, "Cannot find " + astName + " on " + classpath);
		return null;
	}
	
	protected LexCommentList getComments()
	{
		return reader.getComments();
	}
	
	protected boolean isReserved(String name)
	{
		return
			name.startsWith("pre_") ||
			name.startsWith("post_") ||
			name.startsWith("inv_") ||
			name.startsWith("init_") ||
			name.startsWith("measure_") ||
			Settings.release == Release.VDM_10 &&
			(
				name.startsWith("eq_") ||
				name.startsWith("ord_") ||
				name.startsWith("min_") ||
				name.startsWith("max_")
			);
	}
}
