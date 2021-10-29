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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package annotations.po;

import com.fujitsu.vdmj.po.annotations.POAnnotation;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

public class PONullAnnotation extends POAnnotation
{
	private static final long serialVersionUID = 1L;

	public PONullAnnotation(TCIdentifierToken name, POExpressionList args)
	{
		super(name, args);
	}
}
