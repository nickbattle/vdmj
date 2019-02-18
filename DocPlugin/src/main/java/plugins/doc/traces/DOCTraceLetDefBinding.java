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
 
package plugins.doc.traces;

import plugins.doc.definitions.DOCDefinitionList;
import plugins.doc.traces.DOCTraceDefinition;

import com.fujitsu.vdmj.lex.LexLocation;

public class DOCTraceLetDefBinding extends DOCTraceDefinition
{
	private final DOCDefinitionList localDefs;
	private final DOCTraceDefinition body;

	public DOCTraceLetDefBinding(LexLocation location, DOCDefinitionList localDefs, DOCTraceDefinition body)
	{
		super(location);
		this.localDefs = localDefs;
		this.body = body;
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
