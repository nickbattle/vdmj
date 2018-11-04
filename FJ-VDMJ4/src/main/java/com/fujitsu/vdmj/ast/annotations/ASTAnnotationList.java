/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package com.fujitsu.vdmj.ast.annotations;

import java.util.Vector;

import com.fujitsu.vdmj.syntax.SyntaxReader;

public class ASTAnnotationList extends Vector<ASTAnnotation>
{
	private static final long serialVersionUID = 1L;

	public void before(SyntaxReader reader)
	{
		for (ASTAnnotation annotation: this)
		{
			annotation.before(reader);
		}
	}

	public void after(SyntaxReader reader)
	{
		for (ASTAnnotation annotation: this)
		{
			annotation.after(reader);
		}
	}
}
