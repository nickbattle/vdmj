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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.modules;

import java.io.File;
import java.io.Serializable;

import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.ast.annotations.ASTAnnotationList;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.ast.lex.LexCommentList;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.mapper.FileList;

/**
 * A class holding all the details for one module.
 */
public class ASTModule extends ASTNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The module name. */
	public final LexIdentifierToken name;
	/** A list of import declarations. */
	public final ASTModuleImports imports;
	/** A list of export declarations. */
	public final ASTModuleExports exports;
	/** A list of definitions created in the module. */
	public final ASTDefinitionList defs;
	/** A list of source file names for the module. */
	public final FileList files;
	/** True if the module is really a flat file */
	public final boolean isFlat;
	/** List of annotations for the module */
	public ASTAnnotationList annotations;
	/** Comments that occur before the module */
	public LexCommentList comments;

	/**
	 * Create a module from the given name and definitions.
	 */
	public ASTModule(LexIdentifierToken name,
		ASTModuleImports imports,
		ASTModuleExports exports,
		ASTDefinitionList defs)
	{
		this.name = name;
		this.imports = imports;
		this.exports = exports;
		this.defs = defs;
		this.files = new FileList();

		files.add(name.location.file);
		isFlat = false;
	}

	/**
	 * Create a module with a default name from the given definitions.
	 */
	public ASTModule(File file, ASTDefinitionList defs)
	{
		if (defs.isEmpty())
		{
			this.name =	defaultName(new LexLocation());
		}
		else
		{
    		this.name = defaultName(defs.get(0).location);
 		}

		this.imports = null;
		this.exports = null;
		this.defs = defs;
		this.files = new FileList();

		if (file != null)
		{
			files.add(file);
		}
		
		isFlat = true;
	}

	/**
	 * Create a module called DEFAULT with no file and no definitions.
	 */
	public ASTModule()
	{
		this(null, new ASTDefinitionList());
	}

	/**
	 * Generate the default module name.
	 *
	 * @param location	The textual location of the name
	 * @return	The default module name.
	 */
	public static LexIdentifierToken defaultName(LexLocation location)
	{
		return new LexIdentifierToken("DEFAULT", false, location);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("module " + name.name + "\n");

		if (imports != null)
		{
			sb.append("\nimports\n\n");
			sb.append(imports.toString());
		}

		if (exports != null)
		{
			sb.append("\nexports\n\n");
			sb.append(exports.toString());
		}
		else
		{
			sb.append("\nexports all\n\n");
		}

		if (defs != null)
		{
			sb.append("\ndefinitions\n\n");

			for (ASTDefinition def: defs)
			{
				sb.append(def.toString() + "\n");
			}
		}

		sb.append("\nend " + name.name + "\n");

		return sb.toString();
	}

	public void setAnnotations(ASTAnnotationList annotations)
	{
		this.annotations = annotations;
	}

	public void setComments(LexCommentList comments)
	{
		this.comments = comments;
	}
}
