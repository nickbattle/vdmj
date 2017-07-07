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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.definitions;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Delegate;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.scheduler.Lock;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.ClassInvariantListener;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueMap;

/**
 * A class to represent a VDM++ class definition.
 */
public class INClassDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;

	/** The names of the superclasses of this class. */
	public final TCNameList supernames;
	/** The definitions in this class (excludes superclasses). */
	public final INDefinitionList definitions;
	/** This class' class type. */
	public final TCClassType classtype;
	/** Definitions inherited from superclasses. */
	public final INDefinitionList superInheritedDefinitions;
	/** Definitions inherited, but accessed locally. */
	public final INDefinitionList localInheritedDefinitions;
	/** A list of ClassDefinitions for the superclasses. */
	public final INClassList superdefs;
	/** The class invariant operation definition, if any. */
	public final INExplicitOperationDefinition invariant;
	/** True if the class defines any abstract operations or functions. */
	public final boolean isAbstract;

	/** The private or protected static values in the class. */
	private NameValuePairMap privateStaticValues = null;
	/** The public visible static values in the class. */
	private NameValuePairMap publicStaticValues = null;
	
	/** True if the class' static members are initialized. */
	protected boolean staticInit = false;
	/** True if the class' static values are initialized. */
	protected boolean staticValuesInit = false;
	/** True if the class has a sync section with per or mutex defs. */
	public boolean hasPermissions;

	/** A delegate Java object for any native methods. */
	private Delegate delegate = null;
	
	/** A lock for static permission guards - see readObject() */
	public transient Lock guardLock;

	/**
	 * Create a class definition with the given name, list of superclass names,
	 * and list of local definitions.
	 *
	 * @param className
	 * @param supernames
	 * @param definitions
	 */
	public INClassDefinition(TCNameToken className, TCClassType type, TCNameList supernames,
		INDefinitionList definitions, INDefinitionList superInheritedDefinitions,
		INDefinitionList localInheritedDefinitions, INClassList superdefs,
		INExplicitOperationDefinition invariant, boolean isAbstract)
	{
		super(className.getLocation(), null, className);

		this.supernames = supernames;
		this.definitions = definitions;
		this.classtype = type;
		this.superdefs = superdefs;
		this.superInheritedDefinitions = superInheritedDefinitions;
		this.localInheritedDefinitions = localInheritedDefinitions;
		this.invariant = invariant;
		this.isAbstract = isAbstract;

		this.delegate = new Delegate(name.getName(), definitions);
		this.guardLock = new Lock();
	}

	/**
	 * Create a dummy class for the interpreter.
	 */
	public INClassDefinition()
	{
		this(new TCNameToken(new LexLocation(), "CLASS", "DEFAULT"), new TCClassType(new LexLocation(), new TCClassDefinition()),
			new TCNameList(), new INDefinitionList(), new INDefinitionList(), new INDefinitionList(),
			new INClassList(), null, false);

		privateStaticValues = new NameValuePairMap();
		publicStaticValues = new NameValuePairMap();
	}

	/**
	 * Get this class' TCClassType.
	 * @see org.INDefinition.vdmj.definitions.Definition#getType()
	 */
	@Override
	public TCType getType()
	{
		return classtype;
	}

	@Override
	public String toString()
	{
		return	"class " + name.getName() +
				(supernames.isEmpty() ? "" : " is subclass of " + supernames) + "\n" +
				definitions.toString() +
				"end " + name.getName() + "\n";
	}

	@Override
	public INStatement findStatement(int lineno)
	{
		return definitions.findStatement(lineno);
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		return definitions.findExpression(lineno);
	}

	/**
	 * Return the name of the constructor for this class, given a list of
	 * parameter types.
	 */
	public TCNameToken getCtorName(TCTypeList argtypes)
	{
		TCNameToken cname = new TCNameToken(location, name.getName(), name.getName(), false, false);
   		cname.setTypeQualifier(argtypes);
 		return cname;
	}

	/**
	 * Find a constructor definition for this class, given a list of parameter
	 * types.
	 */
	public INDefinition findConstructor(TCTypeList argtypes)
	{
		TCNameToken constructor = getCtorName(argtypes);
		return findName(constructor);
	}

	/**
	 * Find a definition within this class by name.
	 */
	@Override
	public INDefinition findName(TCNameToken sought)
	{
		for (INDefinition d: definitions)
		{
			INDefinition found = d.findName(sought);

			if (found != null)
			{
				return found;
			}
		}
		
		return null;
	}

	/**
	 * Force an initialization of the static functions/operations for the class.
	 */
	public void staticInit(Context ctxt)
	{
		staticInit = false;				// Forced initialization
		staticValuesInit = false;		// Forced initialization

		privateStaticValues = new NameValuePairMap();
		publicStaticValues = new NameValuePairMap();

		setStaticDefinitions(ctxt);
	}

	/**
	 * Force an initialization of the static values for the class.
	 */
	public void staticValuesInit(Context ctxt)
	{
		staticValuesInit = false;		// Forced initialization
		setStaticValues(ctxt);
	}

	/**
	 * Initialize the static functions/operations for the class if it has not
	 * already been done. All statics are added to the global context passed in.
	 * Note that this includes private statics - access is limited by the type
	 * checker.
	 */
	private void setStaticDefinitions(Context initCtxt)
	{
		if (!staticInit)
		{
			staticInit = true;

    		for (INClassDefinition sdef: superdefs)
    		{
    			sdef.setStaticDefinitions(initCtxt);
    		}

    		privateStaticValues = new NameValuePairMap();
    		publicStaticValues = new NameValuePairMap();

    		// We initialize function and operation definitions first as these
    		// can be called by variable initializations.

    		setStaticDefinitions(definitions, initCtxt);
    		setStaticDefinitions(localInheritedDefinitions, initCtxt);
    		
    		try
    		{
				NameValuePairMap members = new NameValuePairMap();
				members.putAll(privateStaticValues);
				members.putAll(publicStaticValues);
				setPermissions(definitions, members, initCtxt);
			}
    		catch (ValueException e)
    		{
    			abort(e);
			}
		}
	}

	private void setStaticDefinitions(INDefinitionList defs, Context initCtxt)
	{
		for (INDefinition d: defs)
		{
			if ((d.isStatic() && d.isFunctionOrOperation()) || d.isTypeDefinition())
			{
				// Note function and operation values are not updatable.
				// TCType invariants are implicitly static, but not updatable

				// The context here is just used for free variables, of
				// which there are none at static func/op creation...

				Context empty = new Context(location, "empty", null);
				NameValuePairList nvl = d.getNamedValues(empty);

				switch (d.accessSpecifier.access)
				{
					case PRIVATE:
					case PROTECTED:
						privateStaticValues.putAllNew(nvl);
						initCtxt.putList(nvl);
						break;

					case PUBLIC:
						publicStaticValues.putAllNew(nvl);
						initCtxt.putList(nvl);
						break;
						
					default:
						break;
				}
			}
		}
	}

	private void setStaticValues(Context initCtxt)
	{
		if (!staticValuesInit)
		{
			staticValuesInit = true;

    		for (INClassDefinition sdef: superdefs)
    		{
    			sdef.setStaticValues(initCtxt);
    		}

    		setStaticValues(localInheritedDefinitions, initCtxt, true);
    		setStaticValues(definitions, initCtxt, false);
		}
	}

	private void setStaticValues(INDefinitionList defs, Context initCtxt, boolean inherit)
	{
		for (INDefinition d: defs)
		{
			NameValuePairList nvl = null;

			if (inherit)
			{
				INInheritedDefinition id = (INInheritedDefinition)d;
				TCNameList names = new TCNameList(d.name);	//d.getVariableNames();
				nvl = new NameValuePairList();

				for (TCNameToken vname: names)
				{
					TCNameToken iname = vname.getModifiedName(id.superdef.name.getModule());
					Value v = initCtxt.check(iname);

					if (v != null)		// TCTypeDefinition names aren't values
					{
						nvl.add(vname, v);
					}
				}
			}
			else
			{
				if (d.isValueDefinition())
				{
					nvl = d.getNamedValues(initCtxt);
				}
				else if (d.isStatic() && d.isInstanceVariable())
				{
					nvl = d.getNamedValues(initCtxt);
				}
			}

			if (d.isValueDefinition())
			{
				// Values are implicitly static, but NOT updatable

				switch (d.accessSpecifier.access)
				{
					case PRIVATE:
					case PROTECTED:
						privateStaticValues.putAllNew(nvl);
						initCtxt.putAllNew(nvl);
						break;

					case PUBLIC:
						publicStaticValues.putAllNew(nvl);
						initCtxt.putAllNew(nvl);
						break;
						
					default:
						break;
				}
			}
			else if (d.isStatic() && d.isInstanceVariable())
			{
				// Static instance variables are updatable

				switch (d.accessSpecifier.access)
				{
					case PRIVATE:
					case PROTECTED:
						privateStaticValues.putAllNew(nvl);
						initCtxt.putAllNew(nvl);
						break;

					case PUBLIC:
						publicStaticValues.putAllNew(nvl);
						initCtxt.putAllNew(nvl);
						break;
						
					default:
						break;
				}
			}
		}
	}

	public Value getStatic(TCNameToken sought)
	{
		TCNameToken local = sought.isExplicit() ? sought : sought.getModifiedName(name.getName());
		Value v = privateStaticValues.get(local);

		if (v == null)
		{
			v = publicStaticValues.get(local);

			if (v == null)
			{
				for (INClassDefinition sdef: superdefs)
				{
					v = sdef.getStatic(local);

					if (v != null)
					{
						break;
					}
				}
			}
		}

		return v;
	}

	public Context getStatics()
	{
		Context ctxt = new Context(location, "Statics", null);
		ctxt.putAll(publicStaticValues);
		ctxt.putAll(privateStaticValues);
		return ctxt;
	}

	public MapValue getOldValues(TCNameList oldnames)
	{
		ValueMap values = new ValueMap();

		for (TCNameToken name: oldnames)
		{
			Value mv = getStatic(name.getNewName()).deref();
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

	/**
	 * Create a new ObjectValue instance of this class. If non-null, the
	 * constructor definition and argument values passed are used, otherwise
	 * the default constructor is used. If there is no default constructor,
	 * just field initializations are made.
	 */
	public ObjectValue newInstance(INDefinition ctorDefinition, ValueList argvals, Context ctxt)
		throws ValueException
	{
		if (isAbstract)
		{
			abort(4000, "Cannot instantiate abstract class " + name, ctxt);
		}

		return makeNewInstance(
			ctorDefinition, argvals, ctxt, new HashMap<TCNameToken, ObjectValue>(), false);
	}

	/**
	 * A method to make new instances, including a list of supertype
	 * objects already constructed to allow for virtual inheritance in
	 * "diamond" inheritance graphs.
	 */
	protected ObjectValue makeNewInstance(
		INDefinition ctorDefinition, ValueList argvals,
		Context ctxt, Map<TCNameToken, ObjectValue> done, boolean nested)
		throws ValueException
	{
		setStaticDefinitions(ctxt.getGlobal());		// When static member := new X()
		setStaticValues(ctxt.getGlobal());			// When static member := new X()

		List<ObjectValue> inherited = new Vector<ObjectValue>();
		NameValuePairMap members = new NameValuePairMap();

		for (INClassDefinition sdef: superdefs)
		{
			// Check the "done" list for virtual inheritance
			ObjectValue obj = done.get(sdef.name);

			if (obj == null)
			{
				obj = sdef.makeNewInstance(null, null, ctxt, done, true);
				done.put(sdef.name, obj);
			}

			inherited.add(obj);
		}

		// NB. we don't use localInheritedDefinitions because we're creating
		// the local definitions in this loop.

		for (INDefinition idef: superInheritedDefinitions)
		{
			// Inherited definitions don't notice when their referenced
			// definition names are updated with type qualifiers.

			if (idef instanceof INInheritedDefinition)
			{
				INInheritedDefinition i = (INInheritedDefinition)idef;
				i.name.setTypeQualifier(i.superdef.name.getTypeQualifier());
			}

			if (idef.isRuntime() && !idef.isSubclassResponsibility())	// eg. TypeDefinitions aren't
			{
				Value v = null;

				for (ObjectValue sobj: inherited)
				{
					v = sobj.get(idef.name, true);

					if (v != null)
					{
						TCNameToken localname = idef.name.getModifiedName(name.getName());

						// In a cascade of classes all overriding a name, we may
						// have already created the local name for the nearest
						// name - superInheritedDefinitions has the nearest first.

						if (members.get(localname) == null)
						{
							members.put(localname, v);
						}

						break;
					}
				}

				if (v == null)
				{
					abort(6, "Constructor for " + name.getName() +
											" can't find " + idef.name, ctxt);
				}
			}
		}

		members.putAll(publicStaticValues);
		members.putAll(privateStaticValues);

		// We create an ObjectContext here so that the member initializers can run in
		// a "self" context that is sensible.

		ObjectValue object = new ObjectValue((TCClassType)getType(), members, inherited, ctxt.threadState.CPU, this);
		Context initCtxt = new ObjectContext(location, "field initializers", ctxt, object);
		
		// We create an empty context to pass for function creation, so that
		// there are no spurious free variables created.

		Context empty = new Context(location, "empty", null);
		NameValuePairMap inheritedNames = new NameValuePairMap();
		inheritedNames.putAll(members);		// Not a clone

		for (INDefinition d: definitions)
		{
			if (!d.isStatic() && d.isFunctionOrOperation())
			{
				NameValuePairList nvpl = d.getNamedValues(empty);

				for (NameValuePair nvp: nvpl)
				{
    				// If there are already overloads inherited, we have to remove them because
					// any local names hide all inherited overloads (like C++).
					
					for (TCNameToken iname: inheritedNames.getOverloadNames(nvp.name))
					{
						initCtxt.remove(iname);
						members.remove(iname);
						inheritedNames.remove(iname);
					}
					
					initCtxt.put(nvp.name, nvp.value);
					members.put(nvp.name, nvp.value);
					
					Value deref = nvp.value.deref();
					
					if (deref instanceof OperationValue)
					{
						OperationValue op = (OperationValue)deref;
						op.setSelf(object);
					}
					else if (deref instanceof FunctionValue)
		 			{
		 				FunctionValue fv = (FunctionValue)deref;
		 				fv.setSelf(object);
		 			}
				}
			}
		}

		for (INDefinition d: definitions)
		{
			if (!d.isStatic() && !d.isFunctionOrOperation())
			{
				NameValuePairList nvpl =
					d.getNamedValues(initCtxt).getUpdatable(null);

				initCtxt.putList(nvpl);
				members.putAll(nvpl);
			}
		}

		setPermissions(definitions, members, initCtxt);
		setPermissions(superInheritedDefinitions, members, initCtxt);

		Value ctor = null;

		if (ctorDefinition == null)
		{
			argvals = new ValueList();
			ctorDefinition = findConstructor(new TCTypeList());
			
			if (ctorDefinition != null)
			{
				ctor = object.get(ctorDefinition.name, false);
			}
		}
		else
		{
     		ctor = object.get(ctorDefinition.name, false);
		}

		if (Settings.dialect ==	Dialect.VDM_RT)
		{
			CPUValue cpu = object.getCPU();

			if (cpu != null)
			{
				cpu.deploy(object);
			}
		}

		if (ctor != null)	// Class may have no constructor defined
		{
     		OperationValue ov = ctor.operationValue(ctxt);

    		ObjectContext ctorCtxt = new ObjectContext(
    				ov.name.getLocation(), name.getName() + " constructor", ctxt, object);
    		
    		if (ctorDefinition.isAccess(Token.PRIVATE) && nested)
    		{
    			ctorDefinition.abort(4163, "Cannot inherit private constructor", ctorCtxt);
    		}

       		ov.eval(ov.name.getLocation(), argvals, ctorCtxt);
		}
		
		// Do invariants and guards after construction, so values fields are set. The
		// invariant does not apply during construction anyway.

		if (invariant != null)
		{
			OperationValue invop = new OperationValue(invariant, null, null, null);
			ClassInvariantListener listener = new ClassInvariantListener(invop);
			
			for (INDefinition d: getInvDefs())
			{
				INClassInvariantDefinition inv = (INClassInvariantDefinition)d;
				
				for (Value v: inv.expression.getValues(initCtxt))
				{
					UpdatableValue uv = (UpdatableValue) v;
					uv.addListener(listener);
				}
			}
			
			object.setListener(listener);
		}

		if (hasPermissions)
		{
    		ObjectContext self = new ObjectContext(
				location, name.getName() + " guards", ctxt, object);

    		for (Entry<TCNameToken, Value> entry: members.entrySet())
			{
				Value v = entry.getValue();

				if (v instanceof OperationValue)
				{
					OperationValue opv = (OperationValue)v;
					opv.prepareGuard(self);
				}
			}
		}

		return object;
	}

	private void setPermissions(
		INDefinitionList defs, NameValuePairMap members, Context initCtxt)
		throws ValueException
	{
		for (INDefinition d: defs)
		{
			while (d instanceof INInheritedDefinition)
			{
				d = ((INInheritedDefinition)d).superdef;
			}

    		if (d instanceof INPerSyncDefinition)
    		{
    			INPerSyncDefinition sync = (INPerSyncDefinition)d;
    			ValueList overloads = members.getOverloads(sync.opname);
    			INExpression exp = sync.getExpression();

    			for (Value op: overloads)
    			{
    				op.operationValue(initCtxt).setGuard(exp, false);
    			}

    			hasPermissions  = true;
    		}
    		else if (d instanceof INMutexSyncDefinition)
    		{
    			INMutexSyncDefinition sync = (INMutexSyncDefinition)d;

    			for (TCNameToken opname: sync.operations)
    			{
    				INExpression exp = sync.getExpression(opname);
    				ValueList overloads = members.getOverloads(opname);

    				for (Value op: overloads)
    				{
    					op.operationValue(initCtxt).setGuard(exp, true);
    				}
    			}

    			hasPermissions = true;
    		}
		}
	}

	private boolean gettingInvDefs = false;

	/**
	 * Get a list of class invariant operation definitions, for this class and
	 * all of its supertypes.
	 */
	public INDefinitionList getInvDefs()
	{
		INDefinitionList invdefs = new INDefinitionList();

		if (gettingInvDefs)
		{
			// reported elsewhere
			return invdefs;
		}

		gettingInvDefs = true;

		for (INClassDefinition d: superdefs)
		{
			invdefs.addAll(d.getInvDefs());
		}

		for (INDefinition d: definitions)
		{
			if (d instanceof INClassInvariantDefinition)
			{
				invdefs.add(d);
			}
		}

		gettingInvDefs = false;
		return invdefs;
	}

	@Override
	public boolean isTypeDefinition()
	{
		return true;
	}

	public boolean hasDelegate()
	{
		return delegate.hasDelegate();
	}

	public Object newInstance()
	{
		return delegate.newInstance();
	}

	public Value invokeDelegate(Object delegateObject, Context ctxt)
	{
		return delegate.invokeDelegate(delegateObject, ctxt);
	}

	public Value invokeDelegate(Context ctxt)
	{
		return delegate.invokeDelegate(null, ctxt);
	}

	private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException
    {
		in.defaultReadObject();
		guardLock = new Lock();
    }
}
