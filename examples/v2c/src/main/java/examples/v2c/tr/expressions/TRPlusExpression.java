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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package examples.v2c.tr.expressions;

public class TRPlusExpression extends TRExpression
{
	private static final long serialVersionUID = 1L;
	private final TRExpression left;
	private final TRExpression right;
	
	public TRPlusExpression(TRExpression left, TRExpression right)
	{
		this.left = left;
		this.right = right;
	}

	@Override
	public String translate()
	{
		return left.translate() + " + " + right.translate();
	}

}
