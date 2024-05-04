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

package com.fujitsu.vdmj.in.modules;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.in.definitions.INRenamedDefinition;
import com.fujitsu.vdmj.in.definitions.INStateDefinition;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.statements.INStatementList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.FileList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Delegate;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;

/**
 * A class holding all the details for one module.
 */
public class INModule extends INNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The module name. */
	public final TCIdentifierToken name;
	public final INDefinitionList defs;
	public final INDefinitionList importdefs;
	public final FileList files;
	
	/** A delegate Java class, if one exists. */
	private Delegate delegate = null;
	/** A delegate Java object, if one exists. */
	private Object delegateObject = null;

	/**
	 * Create a module from the given name and definitions.
	 */
	public INModule(TCIdentifierToken name, INDefinitionList defs, INDefinitionList importdefs, FileList files)
	{
		this.name = name;
		this.defs = defs;
		this.importdefs = importdefs;
		this.files = files;
		this.delegate = new Delegate(name.getName(), defs);
	}

	/**
	 * Create a module with a default name from the given definitions.
	 */
	public INModule(File file, INDefinitionList defs)
	{
		if (defs.isEmpty())
		{
			this.name =	defaultName(LexLocation.ANY);
		}
		else
		{
    		this.name = defaultName(defs.get(0).location);
 		}

		this.defs = defs;
		this.importdefs = new INDefinitionList();
		this.files = new FileList();
		this.delegate = new Delegate(name.getName(), defs);

		if (file != null)
		{
			files.add(file);
		}
	}

	/**
	 * Create a module called DEFAULT with no file and no definitions.
	 */
	public INModule()
	{
		this((File)null, new INDefinitionList());
	}

	/**
	 * Generate the default module name.
	 *
	 * @param location	The textual location of the name
	 * @return	The default module name.
	 */
	public static TCIdentifierToken defaultName(LexLocation location)
	{
		return new TCIdentifierToken(location, "DEFAULT", false);
	}

	/**
	 * Return the module's state context, if any. Modules which define
	 * state produce a {@link Context} object that contains the state field
	 * values. This is independent of the initial context.
	 *
	 * @return	The state context, or null.
	 */
	public Context getStateContext()
	{
		INStateDefinition sdef = defs.findStateDefinition();

		if (sdef != null)
		{
			return sdef.getStateContext();
		}

		return null;
	}

	/**
	 * Initialize the system for execution from this module. The initial
	 * {@link Context} is created, and populated with name/value pairs from the
	 * local definitions and the imported definitions. If state is defined
	 * by the module, this is also initialized, creating the state Context.
	 *
	 * @return True if initialized OK.
	 */
	public Set<ContextException> initialize(StateContext initialContext)
	{
		Set<ContextException> trouble = new HashSet<ContextException>();

		for (INDefinition d: importdefs)
		{
			if (d instanceof INRenamedDefinition)
			{
				try
				{
					initialContext.putList(d.getNamedValues(initialContext));
				}
				catch (ContextException e)
				{
					trouble.add(e);		// Carry on...
					
					if (e.isStackOverflow() || e.isUserCancel())
					{
						trouble.clear();
						trouble.add(e);
						return trouble;
					}
				}
			}
		}

		for (INDefinition d: defs)
		{
			try
			{
				// Create a local context to identify the init location for this defn.
				Context ctxt = new Context(d.location, "<init> " + d, initialContext); 
				
				initialContext.putList(d.getNamedValues(ctxt));
			}
			catch (ContextException e)
			{
				trouble.add(e);		// Carry on...
				
				if (e.isStackOverflow() || e.isUserCancel())
				{
					trouble.clear();
					trouble.add(e);
					return trouble;
				}
			}
		}

		try
		{
			INStateDefinition sdef = defs.findStateDefinition();

			if (sdef != null)
			{
				sdef.initState(initialContext);
			}
		}
		catch (ContextException e)
		{
			trouble.add(e);		// Carry on...
			
			if (e.isStackOverflow() || e.isUserCancel())
			{
				trouble.clear();
				trouble.add(e);
				return trouble;
			}
		}

		return trouble;
	}

	/**
	 * Find all {@link INStatement} in the module that start on a given line.
	 *
	 * @param file The file to search for.
	 * @param lineno The line number to search for.
	 * @return	The {@link TCStatement} on that line, or null.
	 */
	public INStatementList findStatements(File file, int lineno)
	{
		// The DEFAULT module can include definitions from many files,
		// so we have to consider each definition's file before searching
		// within that for the statement.

		for (INDefinition d: defs)
		{
			if (d.location.file.equals(file))
			{
				INStatementList stmts = d.findStatements(lineno);

				if (!stmts.isEmpty())
				{
					return stmts;
				}
			}
		}

		return null;
	}

	/**
	 * Find all {@link INExpression} in the module that start on a given line.
	 *
	 * @param file The file to search for.
	 * @param lineno The line number to search for.
	 * @return	The {@link TCExpression} on that line, or null.
	 */
	public INExpressionList findExpressions(File file, int lineno)
	{
		// The DEFAULT module can include definitions from many files,
		// so we have to consider each definition's file before searching
		// within that for the expression.

		for (INDefinition d: defs)
		{
			if (d.location.file.equals(file))
			{
				INExpressionList exps = d.findExpressions(lineno);

				if (!exps.isEmpty())
				{
					return exps;
				}
			}
		}

		return null;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("module " + name.getName() + "\n");

		if (defs != null)
		{
			sb.append("\ndefinitions\n\n");

			for (INDefinition def: defs)
			{
				sb.append(def.toString() + "\n");
			}
		}

		sb.append("\nend " + name.getName() + "\n");

		return sb.toString();
	}

	public boolean hasDelegate()
	{
		if (delegate.hasDelegate())
		{
			if (delegateObject == null)
			{
				delegateObject = delegate.newInstance();
			}

			return true;
		}

		return false;
	}

	public Value invokeDelegate(Context ctxt, Token section)
	{
		return delegate.invokeDelegate(delegateObject, ctxt, section);
	}
}
