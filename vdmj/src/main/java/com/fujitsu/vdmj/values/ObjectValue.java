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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.values;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.scheduler.Lock;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public class ObjectValue extends Value
{
	private static final long serialVersionUID = 1L;

	private static int nextObjectReference = 0;

	public final int objectReference;
	public final TCClassType type;
	public final NameValuePairMap members;
	public final List<ObjectValue> superobjects;
	public final INClassDefinition classdef;

	public ClassInvariantListener invlistener = null;

	public transient Lock guardLock;
	private transient CPUValue CPU;
	private Object delegateObject = null;
	private int periodicCount = 0;
	private int periodicOverlaps = 0;

	public ObjectValue(TCClassType type,
		NameValuePairMap members, List<ObjectValue> superobjects, CPUValue cpu, INClassDefinition classdef)
	{
		this.objectReference = getReference();
		this.type = type;
		this.members = members;
		this.superobjects = superobjects;
		this.classdef = classdef;
		
		this.CPU = cpu;
		this.guardLock = new Lock();

		setSelf(this);
	}

	private static synchronized int getReference()
	{
		return ++nextObjectReference;
	}

	private void setSelf(ObjectValue self)
	{
		for (NameValuePair nvp: members.asList())
 		{
			Value deref = nvp.value.deref();

 			if (deref instanceof OperationValue)
 			{
 				OperationValue ov = (OperationValue)deref;
 				ov.setSelf(self);
 			}
 			else if (deref instanceof FunctionValue)
 			{
 				FunctionValue fv = (FunctionValue)deref;
 				fv.setSelf(self);
 			}
 		}

		for (ObjectValue obj: superobjects)
		{
			obj.setSelf(self);
		}
	}

	@Override
	public ObjectValue objectValue(Context ctxt)
	{
		return this;
	}

	public TCTypeList getBaseTypes()
	{
		TCTypeList basetypes = new TCTypeList();

		if (superobjects.isEmpty())
		{
			basetypes.add(type);
		}
		else
		{
    		for (ObjectValue sup: superobjects)
    		{
    			basetypes.addAll(sup.getBaseTypes());
    		}
		}

		return basetypes;
	}

	@Override
	public Value getUpdatable(ValueListenerList listeners)
	{
		for (Entry<TCNameToken, Value> m: members.entrySet())
		{
			Value v = m.getValue();
			
			if (v instanceof UpdatableValue && listeners != null)
			{
				// Update one level of listeners in-place (see UpdatableValue)
				UpdatableValue uv = (UpdatableValue)v;
				uv.addListeners(listeners);		// Concurrent update with listener invocations
			}
		}

//		for (Entry<LexNameToken, Value> m: members.entrySet())
//		{
//			Value v = m.getValue();
//
//			if (v.deref() instanceof ObjectValue)
//			{
//				// Don't recurse into inner objects, just mark field itself
//				m.setValue(UpdatableValue.factory(v, listeners));
//			}
//			else if (v.deref() instanceof FunctionValue)
//			{
//				// Ignore function members
//			}
//			else if (v.deref() instanceof OperationValue)
//			{
//				// Ignore operation members
//			}
//			else
//			{
//				m.setValue(v.getUpdatable(listeners));
//			}
//		}

		
		return UpdatableValue.factory(this, listeners);
	}

	public OperationValue getThreadOperation(Context ctxt) throws ValueException
	{
		return get(type.classdef.name.getThreadName(), false).operationValue(ctxt);
	}

	public synchronized int incPeriodicCount()
	{
		if (periodicCount > 0)
		{
			periodicOverlaps++;
		}

		periodicCount++;
		return periodicOverlaps;
	}

	public synchronized void decPeriodicCount()
	{
		periodicCount--;
	}

	public synchronized Value get(TCNameToken field, boolean explicit)
	{
		TCNameToken localname =
			explicit ? field : field.getModifiedName(type.name.getName());

		// This is another case where we have to iterate with equals()
		// rather than using the map's hash, because the hash doesn't
		// take account of the TypeComparator looseness when comparing
		// qualified names. Not very efficient... so we try a raw get
		// first.

		Value rv = members.get(localname);

		if (rv == null)
		{
    		for (TCNameToken var: members.keySet())
    		{
    			if (var.equals(localname))
    			{
    				rv = members.get(var);
    				break;
    			}
    		}
		}

		if (rv != null)
		{
			return rv;
		}

		for (ObjectValue svalue: superobjects)
		{
			rv = svalue.get(field, explicit);

			if (rv != null)
			{
				return rv;
			}
		}

		return null;
	}

	public ValueList getOverloads(TCNameToken field)
	{
		ValueList list = new ValueList();

		// This is another case where we have to iterate with matches()
		// rather than using the map's hash, because the hash includes the
		// overloaded type qualifiers...

		for (TCNameToken var: members.keySet())
		{
			if (var.matches(field))		// Ignore type qualifiers
			{
				list.add(members.get(var));
			}
		}

		if (!list.isEmpty())
		{
			return list;	// Only names from one level
		}

		for (ObjectValue svalue: superobjects)
		{
			list = svalue.getOverloads(field);

			if (!list.isEmpty())
			{
				return list;
			}
		}

		return list;
	}

	public NameValuePairMap getMemberValues()
	{
		NameValuePairMap nvpm = new NameValuePairMap();

		for (ObjectValue svalue: superobjects)
		{
			nvpm.putAll(svalue.getMemberValues());
		}

		nvpm.putAll(members);
		return nvpm;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

    		if (val instanceof ObjectValue)
    		{
    			return ((ObjectValue) val).objectReference == this.objectReference;
    		}
		}

		return false;
	}

	private boolean inToString = false;

	@Override
	public String toString()
	{
		if (inToString)
		{
			return "{#" + objectReference + " recursive}";
		}

		inToString = true;
		StringBuilder sb = new StringBuilder();
		sb.append(type.toString());
		sb.append("{#" + objectReference);

		for (TCNameToken name: members.keySet())
		{
			Value ov = members.get(name);
			Value v = ov.deref();

			if (!(v instanceof FunctionValue) &&
				!(v instanceof OperationValue))
			{
				sb.append(", ");
				sb.append(name.getName());

				if (ov instanceof UpdatableValue)
				{
					sb.append(":=");
				}
				else
				{
					sb.append("=");
				}

				sb.append(v.toString());
			}
		}

		if (!superobjects.isEmpty())
		{
			sb.append(", ");
			sb.append(Utils.listToString(superobjects));
		}

		sb.append("}");
		inToString = false;
		return sb.toString();
	}

	@Override
	public int hashCode()
	{
		// return type.hashCode() + objectReference + superobjects.hashCode();
		return objectReference;
	}

	@Override
	public String kind()
	{
		return type.toString();
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		Value conv = convertToHierarchy(to);

		if (conv != null)
		{
			return conv;
		}

		// This will fail...
		return super.convertValueTo(to, ctxt, done);
	}

	private Value convertToHierarchy(TCType to)
	{
		if (to.equals(type))
		{
			return this;
		}

		for (ObjectValue svalue: superobjects)
		{
			Value conv = svalue.convertToHierarchy(to);

			if (conv != null)
			{
				return this;	// NB. not the subtype
			}
		}

		return null;
	}

	@Override
	public Object clone()
	{
		return deepCopy();
	}

	private ObjectValue mycopy = null;

	@Override
	public ObjectValue shallowCopy()
	{
		if (mycopy != null)
		{
			return mycopy;
		}

		mycopy = new ObjectValue(type,
					new NameValuePairMap(), new Vector<ObjectValue>(), CPU, classdef);

		List<ObjectValue> supers = mycopy.superobjects;
		NameValuePairMap memcopy = mycopy.members;

   		for (ObjectValue sobj: superobjects)
   		{
   			supers.add(	// TCType skeleton only...
   				new ObjectValue(sobj.type,
   					new NameValuePairMap(), new Vector<ObjectValue>(), sobj.CPU, classdef));
   		}

		for (TCNameToken name: members.keySet())
		{
			Value mv = members.get(name);

			if (mv.deref() instanceof ObjectValue)
			{
				ObjectValue om = (ObjectValue)mv.deref();
				memcopy.put(name, om.shallowCopy());
			}
			else
			{
				memcopy.put(name, (Value)mv.clone());
			}
		}

		mycopy.setSelf(mycopy);

		ObjectValue rv = mycopy;
		mycopy = null;
		return rv;
	}

	@Override
	public ObjectValue deepCopy()
	{
		try
		{
			// This is slow, but it has the advantage that Value copies,
			// such as the parent and subclass copies of the same
			// variable, are preserved as the same variable rather than
			// being split, as they are in naive object copies.

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(this);
			oos.close();

			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(is);
			ObjectValue result = (ObjectValue)ois.readObject();

			result.setSelf(result);
			return result;
		}
		catch (Exception e)
		{
			throw new InternalException(5, "Illegal clone: " + e);
		}
	}
	
	public MapValue getOldValues(TCNameList oldnames)
	{
		ValueMap values = new ValueMap();
		ObjectContext ctxt = new ObjectContext(type.location, "Old Object Creation", null, this);

		for (TCNameToken name: oldnames)
		{
			Value mv = ctxt.check(name.getNewName()).deref();
			SeqValue sname = new SeqValue(name.getName());

			if (mv instanceof ObjectValue)
			{
				ObjectValue om = (ObjectValue)mv;
				values.put(sname, om.deepCopy());
			}
			else
			{
				values.put(sname, (Value)mv.clone());
			}
		}

		return new MapValue(values);
	}
	

//	private void writeObject(ObjectOutputStream out)
//		throws IOException
//	{
//		out.defaultWriteObject();
//	}

	private void readObject(ObjectInputStream in)
		throws ClassNotFoundException, IOException
    {
		in.defaultReadObject();
		CPU = CPUValue.vCPU;
		guardLock = new Lock();
    }

	public synchronized void setCPU(CPUValue cpu)
	{
		CPU = cpu;
	}

	public synchronized CPUValue getCPU()
	{
		return CPU == null ? CPUValue.vCPU : CPU;
	}

	public boolean hasDelegate()
	{
		if (classdef.hasDelegate())
		{
			if (delegateObject == null)
			{
				delegateObject = classdef.newInstance();
			}

			return true;
		}

		return false;
	}

	public Value invokeDelegate(Context ctxt)
	{
		return classdef.invokeDelegate(delegateObject, ctxt);
	}

	public static void init()
	{
		nextObjectReference = 0;
	}

	public void setListener(ClassInvariantListener listener)
	{
		invlistener = listener;
		listener.invopvalue.setSelf(this);
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseObjectValue(this, arg);
	}
}
