/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.pog;

public enum POType
{
	MAP_APPLY("map apply"),
	FUNC_APPLY("function apply", true),
	SEQ_APPLY("sequence apply", true),
	FUNC_POST_CONDITION("post condition"),
	FUNC_SATISFIABILITY("function satisfiability", true),
	FUNC_PATTERNS("function parameter patterns"),
	LET_BE_EXISTS("let be st existence"),
	UNIQUE_EXISTENCE("unique existence binding", true),
	FUNC_ITERATION("function iteration", true),
	MAP_ITERATION("map iteration", true),
	FUNC_COMPOSE("function compose", true),
	MAP_COMPOSE("map compose", true),
	NON_EMPTY_SET("non-empty set", true),
	NON_EMPTY_SEQ("non-empty sequence", true),
	NON_ZERO("non-zero", true),
	FINITE_MAP("finite map", true),
	FINITE_SET("finite set", true),
	MAP_COMPATIBLE("map compatible", true),
	MAP_SEQ_OF_COMPATIBLE("map sequence compatible", true),
	MAP_SET_OF_COMPATIBLE("map set compatible", true),
	SEQ_MODIFICATION("sequence modification", true),
	VALUE_BINDING("value binding", true),
	SUB_TYPE("subtype", true),
	CASES_EXHAUSTIVE("cases exhaustive", true),
	INVARIANT("type invariant", true),
	RECURSIVE("recursive function"),
	STATE_INVARIANT("state invariant"),
	LOOP_INVARIANT("loop invariant"),
	LOOP_MEASURE("loop measure"),
	OP_PRE_CONDITION("operation pre condition"),
	OP_POST_CONDITION("operation post condition"),
	SPEC_PRE_CONDITION("specification precondition"),
	OPERATION_PATTERNS("operation parameter patterns"),
	OP_SATISFIABILITY("operation satisfiability", true),
	STMT_SATISFIABILITY("statement satisfiability", true),
	SET_MEMBER("set membership", true),
	SEQ_MEMBER("sequence membership", true),
	ORDERED("ordered"),
	STRICT_ORDER("strict order", true),
	TOTAL_ORDER("total order", true),
	EQUIV_RELATION("equivalence relation", true),
	TOTAL_FUNCTION("total function"),
	INV_SATISFIABILITY("invariant satisfiability", true),
	THEOREM("theorem"),
	STATE_INIT("state init", true);

	private String kind;
	private boolean standAlone;

	POType(String kind)
	{
		this.kind = kind;
		this.standAlone = false;
	}

	POType(String kind, boolean standAlone)
	{
		this.kind = kind;
		this.standAlone = standAlone;
	}

	@Override
	public String toString()
	{
		return kind;
	}
	
	public boolean isStandAlone()
	{
		return standAlone;
	}
}
