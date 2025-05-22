/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package annotations.ast;

import com.fujitsu.vdmj.ast.annotations.ASTAnnotation;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;

/**
 * This annotation is created and associated with the AST node concerned, without
 * the need for a mapping file, since the AST is always processed first. The
 * ExamplePlugin only processes the AST tree, so that is sufficient, but more
 * complex annotations could map this annotation into TC or IN or PO versions,
 * which then can be mapped to the plugin's ClassMapper tree (if any).
 */
public class ASTNoCheckAnnotation extends ASTAnnotation
{
	private static final long serialVersionUID = 1L;

	public ASTNoCheckAnnotation(LexIdentifierToken name)
	{
		super(name);
	}
}
