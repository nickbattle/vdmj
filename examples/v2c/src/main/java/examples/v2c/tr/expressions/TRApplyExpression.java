/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package examples.v2c.tr.expressions;

public class TRApplyExpression extends TRExpression
{
	private static final long serialVersionUID = 1L;
	private final TRExpression root;
	private final TRExpressionList args;
	
	public TRApplyExpression(TRExpression root, TRExpressionList args)
	{
		this.root = root;
		this.args = args;
	}

	@Override
	public String translate()
	{
		return root.translate() + "(" + args.translate() + ")";
	}
}
