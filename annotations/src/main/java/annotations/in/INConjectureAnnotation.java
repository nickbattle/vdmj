/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package annotations.in;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INNilExpression;
import com.fujitsu.vdmj.messages.ConjectureProcessor;
import com.fujitsu.vdmj.messages.RTValidator;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.util.JSONWriter;

public abstract class INConjectureAnnotation extends INAnnotation implements ConjectureProcessor
{
	private static final long serialVersionUID = 1L;

	protected final String cname;
	protected final String e1;
	protected final INExpression condition;
	protected final String e2;
	protected final long delay;
	protected final boolean match;

	public INConjectureAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
		
		this.cname = "C" + (++counter);
		this.e1 = args.get(0).toString();
		this.condition = args.get(1) instanceof INNilExpression ? null : args.get(1);
		this.e2 = args.get(2).toString();
		this.delay = Long.parseLong(args.get(3).toString());
		this.match = Boolean.parseBoolean(args.get(4).toString());
	}

	protected static class Occurrence
	{
		public final long i1;
		public final Map<String, String> record;
		
		public Occurrence(long i1, Map<String, String> record)
		{
			this.i1 = i1;
			this.record = record;
		}
		
		public long time()
		{
			return Long.parseLong(record.get("time"));
		}
		
		public String get(String key)
		{
			return record.get(key);
		}
	}
	
	protected static class Failure
	{
		public final INConjectureAnnotation annotation;
		public final Occurrence first;
		public final Occurrence second;
		
		public Failure(INConjectureAnnotation annotation, Occurrence first, Occurrence second)
		{
			this.annotation = annotation;
			this.first = first;
			this.second = second;
		}
		
		public Failure(INConjectureAnnotation annotation, Occurrence occ)
		{
			this.annotation = annotation;
			this.first = occ;
			this.second = null;
		}
		
		public String toJSON()
		{
			StringWriter sw = new StringWriter();
			JSONWriter jw = new JSONWriter(new PrintWriter(sw));
			Map<String, Object> json = new LinkedHashMap<String, Object>();
			
			json.put("name", annotation.cname);
			json.put("status", false);
			json.put("expression", annotation.toString());
			
			Map<String, Object> event1 = new LinkedHashMap<String, Object>();
			event1.put("kind", first.get(RTValidator.KIND));
			event1.put("opname", first.get(RTValidator.OPNAME));
			event1.put("time", Long.parseLong(first.get("time")));
			event1.put("thid", Long.parseLong(first.get("id")));
			json.put("source", event1);
			
			if (second != null)
			{
				Map<String, Object> event2 = new LinkedHashMap<String, Object>();
				event2.put("kind", second.get(RTValidator.KIND));
				event2.put("opname", second.get(RTValidator.OPNAME));
				event2.put("time", Long.parseLong(second.get("time")));
				event2.put("thid", Long.parseLong(second.get("id")));
				json.put("destination", event2);
			}
			
			jw.writeObject(json);
			return sw.getBuffer().toString();
		}
	}

	protected final List<Occurrence> occurrences = new Vector<Occurrence>();
	protected final List<Failure> failures = new Vector<Failure>();
	protected long i1 = 0;
	protected long i2 = 0;
	
	private static int counter = 0;		// Names conjectures, C1, C2, etc

	@Override
	public void processReset()
	{
		occurrences.clear();
		failures.clear();
		i1 = 0;
		i2 = 0;
	}

	public static void doInit()
	{
		counter = 0;
	}
	
	@Override
	public int processComplete(File violations) throws IOException
	{
		PrintWriter pw = new PrintWriter(new FileWriter(violations, true));
		
		try
		{
			if (failures.isEmpty())
			{
				Map<String, Object> json = new HashMap<String, Object>();
				json.put("status", true);
				json.put("name", cname);
				json.put("expression", toString());
				new JSONWriter(pw).writeObject(json);
			}
			
			for (Failure failure: failures)
			{
				pw.println(failure.toJSON());
			}
		}
		finally
		{
			pw.close();
		}
		
		return failures.size();
	}
	
	protected boolean checkCondition(Context ctxt)
	{
		if (condition == null)
		{
			return true;
		}
		
		try
		{
			return condition.eval(ctxt).boolValue(ctxt);
		}
		catch (ValueException e)
		{
			return false;			// Probably value is not boolean
		}
		catch (ContextException e)
		{
			if (e.number == 4034)	// Name not in scope => no InstVarChange events
			{
				return false;
			}
			
			throw e;
		}
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("#");
		sb.append(name);
		sb.append("(");
		sb.append(e1);
		sb.append(", ");
		sb.append(condition);
		sb.append(", ");
		sb.append(e2);
		sb.append(", ");
		sb.append(delay);
		sb.append(", ");
		sb.append(match);
		sb.append(")");
		
		return sb.toString();
	}
}
