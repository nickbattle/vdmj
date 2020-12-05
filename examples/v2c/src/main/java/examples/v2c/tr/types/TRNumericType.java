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

package examples.v2c.tr.types;

import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;

public class TRNumericType extends TRType
{
	private static final long serialVersionUID = 1L;
	private final TCType type;

	public TRNumericType(TCNaturalType type)
	{
		this.type = type;
	}

	public TRNumericType(TCNaturalOneType type)
	{
		this.type = type;
	}

	public TRNumericType(TCIntegerType type)
	{
		this.type = type;
	}

	public TRNumericType(TCRationalType type)
	{
		this.type = type;
	}

	public TRNumericType(TCRealType type)
	{
		this.type = type;
	}
	
	@Override
	public String translate()
	{
		switch (type.toString())
		{
			case "nat": case "int": case "nat1":
				return "long";
				
			case "rat": case "real":
				return "double";
				
			default:
				throw new RuntimeException("Untranslatable type " + type);
		}
	}
}
