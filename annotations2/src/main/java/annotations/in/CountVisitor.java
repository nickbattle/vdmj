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

package annotations.in;

import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public class CountVisitor extends ValueVisitor<Long, Object>
{
	@Override
	public Long caseValue(Value node, Object arg)
	{
		return 1L;
	}
	
	@Override
	public Long caseSeqValue(SeqValue node, Object arg)
	{
		long count = 0;
		
		for (Value v: node.values)
		{
			count += v.apply(this, arg);
		}
		
		return count;
	}
}
