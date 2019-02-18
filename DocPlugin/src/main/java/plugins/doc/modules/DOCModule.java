/*******************************************************************************
 *
 *	Copyright (c) 2019 Paul Chisholm
 *
 *	Author: Paul Chisholm
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
 
package plugins.doc.modules;

import plugins.doc.DOCNode;
import plugins.doc.annotations.DOCAnnotationList;
import plugins.doc.definitions.DOCDefinitionList;
import plugins.doc.lex.DOCIdentifierToken;
import plugins.doc.modules.DOCModuleExports;
import plugins.doc.modules.DOCModuleImports;

import com.fujitsu.vdmj.ast.lex.LexCommentList;
import com.fujitsu.vdmj.mapper.FileList;

public class DOCModule extends DOCNode
{
	private static final long serialVersionUID = 1L;
	private final DOCAnnotationList annotations;
	private final DOCIdentifierToken name;
	private final DOCModuleImports imports;
	private final DOCModuleExports exports;
	private final DOCDefinitionList defs;
	private final FileList files;
	private final boolean isFlat;

	private LexCommentList comments;

	public DOCModule(DOCAnnotationList annotations, DOCIdentifierToken name, DOCModuleImports imports, DOCModuleExports exports, DOCDefinitionList defs, FileList files, boolean isFlat)
	{
		super();
		this.annotations = annotations;
		this.name = name;
		this.imports = imports;
		this.exports = exports;
		this.defs = defs;
		this.files = files;
		this.isFlat = isFlat;
	}

	public void setComments(LexCommentList comments)
	{
		this.comments = comments;
	}

	@Override
	public void extent(int maxWidth)
	{
		return;
	}
	
	@Override
	public String toHTML(int indent)
	{
		return null;
	}
}
