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
 
package plugins.doc.statements;

import plugins.doc.patterns.DOCPatternBind;
import plugins.doc.statements.DOCStatement;

public class DOCTixeStmtAlternative
{
	private final DOCPatternBind patternBind;
	private final DOCStatement statement;

	public DOCTixeStmtAlternative(DOCPatternBind patternBind, DOCStatement statement)
	{
		super();
		this.patternBind = patternBind;
		this.statement = statement;
	}

	public void extent(int maxWidth)
	{
		return;
	}
	
	public String toHTML(int indent)
	{
		return null;
	}
}
