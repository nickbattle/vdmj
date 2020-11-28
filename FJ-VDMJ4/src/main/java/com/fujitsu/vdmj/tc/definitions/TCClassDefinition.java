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

package com.fujitsu.vdmj.tc.definitions;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.statements.TCClassInvariantStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * A class to represent a VDM++ class definition.
 */
public class TCClassDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;

	/** The names of the superclasses of this class. */
	public final TCNameList supernames;
	
	/** The definitions in this class (excludes superclasses). */
	public final TCDefinitionList definitions;

	/** Definitions inherited from superclasses. */
	public TCDefinitionList superInheritedDefinitions = null;
	
	/** Definitions inherited, but accessed locally. */
	public TCDefinitionList localInheritedDefinitions = null;
	
	/** The combination of all inherited definitions. */
	public TCDefinitionList allInheritedDefinitions = null;

	/** A list of ClassTypes for the superclasses. */
	public TCTypeList supertypes = null;
	
	/** A list of ClassDefinitions for the superclasses. */
	public TCClassList superdefs = null;
	
	/** This class' class type. */
	public TCClassType classtype = null;

	/** Used during the linkage of the class hierarchy. */
	private enum Setting { UNSET, INPROGRESS, DONE }
	volatile private Setting settingHierarchy = Setting.UNSET;

	/** The class invariant operation definition, if any. */
	public TCExplicitOperationDefinition invariant = null;
	/** True if the class defines any abstract operations or functions. */
	public boolean isAbstract = false;
	/** True if the class has any constructors at all. */
	public boolean hasConstructors = false;
	/** True if the class has a sync section with per or mutex defs. */
	public boolean hasPermissions = false;

	/** Temp location of expression calling findName, for error reporting */
	private TCExpression findFrom = null;
	
	
	/**
	 * Create a class definition with the given name, list of superclass names,
	 * and list of local definitions.
	 * @param className
	 * @param supernames
	 * @param definitions
	 */
	public TCClassDefinition(TCAnnotationList annotations, TCNameToken className, TCNameList supernames, TCDefinitionList definitions)
	{
		super(Pass.DEFS, className.getLocation(), className, NameScope.CLASSNAME);

		this.annotations = annotations;
		this.supernames = supernames;
		this.definitions = definitions;

		this.used = true;
		this.superdefs = new TCClassList();
		this.supertypes = new TCTypeList();
		this.superInheritedDefinitions = new TCDefinitionList();
		this.localInheritedDefinitions = new TCDefinitionList();
		this.allInheritedDefinitions = new TCDefinitionList();

		// Classes are all effectively public types
		this.setAccessSpecifier(new TCAccessSpecifier(false, false, Token.PUBLIC, false));
		this.definitions.setClassDefinition(this);
	}

	public TCClassDefinition(TCNameToken className, TCNameList supernames, TCDefinitionList definitions)
	{
		this(null, className, supernames, definitions);		// No annotations
	}
	
	/**
	 * Create an empty dummy class for the interpreter.
	 */
	public TCClassDefinition()
	{
		this(null, new TCNameToken(new LexLocation(), "CLASS", "DEFAULT", false, false),
			 new TCNameList(),
			 new TCDefinitionList());
	}

	/**
	 * Link the class hierarchy and generate the invariant operation.
	 * @see com.fujitsu.vdmj.ast.definitions.TCDefinition#implicitDefinitions(com.fujitsu.vdmj.typechecker.Environment)
	 */
	@Override
	public void implicitDefinitions(Environment publicClasses)
	{
		setInherited(publicClasses);
		setInheritedDefinitions();

		invariant = getInvDefinition();

		if (invariant != null)
		{
			invariant.setClassDefinition(this);
		}
	}

	/**
	 * Check definitions for illegal function/operation overloading and
	 * for illegal overriding.
	 */
	public void checkOver()
	{
		int inheritedThreads = 0;
		checkOverloads();

		List<TCDefinitionList> superlist = new Vector<TCDefinitionList>();

		for (TCDefinition def: superdefs)
		{
			TCClassDefinition superdef = (TCClassDefinition)def;
			TCDefinitionList inheritable = superdef.getInheritable();
			superlist.add(inheritable);

			if (checkOverrides(inheritable))
			{
				inheritedThreads++;
			}
		}

		if (inheritedThreads > 1)
		{
			report(3001, "Class inherits thread definition from multiple supertypes");
		}

		checkAmbiguities(superlist);
	}

	/**
	 * Create the class hierarchy. This populates the superdefs and supertypes
	 * fields, while first calling setInherited of the superclasses first. The
	 * settingHierarchy field is used to avoid loops.
	 *
	 * @param base
	 */
	private void setInherited(Environment base)
	{
		switch (settingHierarchy)
		{
			case UNSET:
				settingHierarchy = Setting.INPROGRESS;
				break;

			case INPROGRESS:
				report(3002, "Circular class hierarchy detected: " + name);
				return;

			case DONE:
				return;
		}

		definitions.implicitDefinitions(base);

		for (TCNameToken supername: supernames)
		{
			TCDefinition def = base.findType(supername, null);

			if (def == null)
			{
				report(3003, "Undefined superclass: " + supername);
			}
			else if (def instanceof TCCPUClassDefinition)
			{
				report(3298, "Cannot inherit from CPU");
			}
			else if (def instanceof TCBUSClassDefinition)
			{
				report(3299, "Cannot inherit from BUS");
			}
			else if (def instanceof TCSystemDefinition)
			{
				report(3278, "Cannot inherit from system class " + supername);
			}
			else if (def instanceof TCClassDefinition)
			{
				TCClassDefinition superdef = (TCClassDefinition)def;
				superdef.setInherited(base);
				superdefs.add(superdef);
				supertypes.add(superdef.getType());
			}
			else
			{
				report(3004, "Superclass name is not a class: " + supername);
			}
		}

		settingHierarchy = Setting.DONE;
		return;
	}

	/**
	 * Check for illegal overrides, given a list of inherited functions and
	 * operations.
	 *
	 * @param inheritable
	 */
	private boolean checkOverrides(TCDefinitionList inheritable)
	{
		boolean inheritedThread = false;

		for (TCDefinition indef: inheritable)
		{
			if (indef.name.getName().equals("thread"))
			{
				inheritedThread = true;
				continue;	// No other checks needed for threads
			}

			TCNameToken localName = indef.name.getModifiedName(name.toString());

			TCDefinition override =
				definitions.findName(localName,	NameScope.NAMESANDSTATE);

			if (override == null)
			{
				override = definitions.findType(localName, null);
			}

			if (override != null)
			{
				if (!indef.kind().equals(override.kind()))
				{
					override.report(3005, "Overriding a superclass member of a different kind: " + override.name);
					override.detail2("This", override.kind(), "Super", indef.kind());
				}
				else if (override.accessSpecifier.narrowerThan(indef.accessSpecifier))
				{
					override.report(3006, "Overriding definition reduces visibility");
					override.detail2("This", override.name, "Super", indef.name);
				}
				else if (override.isPure() != indef.isPure())
				{
					override.report(3341, "Overriding definition must " + (override.isPure() ? "not" : "also") + " be pure");
				}
				else
				{
					TCType to = indef.getType();
					TCType from = override.getType();

					// Note this uses the "parameters only" comparator option

					if (!TypeComparator.compatible(to, from, true))
					{
						override.report(3007, "Overriding member incompatible type: " + override.name);
						override.detail2("This", override.getType(), "Super", indef.getType());
					}
				}
			}
		}

		return inheritedThread;
	}

	private void checkAmbiguities(List<TCDefinitionList> superlist)
	{
		int count = superlist.size();

		for (int i=0; i<count; i++)
		{
			TCDefinitionList defs = superlist.get(i);

			for (int j=i+1; j<count; j++)
			{
				TCDefinitionList defs2 = superlist.get(j);
				checkAmbiguities(defs, defs2);
    		}
		}
	}

	private void checkAmbiguities(TCDefinitionList defs, TCDefinitionList defs2)
	{
		for (TCDefinition indef: defs)
		{
			TCNameToken localName = indef.name.getModifiedName(name.toString());

			for (TCDefinition indef2: defs2)
			{
    			if (!indef.location.equals(indef2.location) &&
    				indef.kind().equals(indef2.kind()))
    			{
    				TCNameToken localName2 = indef2.name.getModifiedName(name.toString());

    				if (localName.equals(localName2))
    				{
    					TCDefinition override =
    						definitions.findName(localName,	NameScope.NAMESANDSTATE);

    					if (override == null)	// OK if we override the ambiguity
    					{
        					report(3276, "Ambiguous definitions inherited by " + name);
        					detail("1", indef.name + " " + indef.location);
        					detail("2", indef2.name + " " + indef2.location);
    					}
    				}
    			}
			}
		}
	}

	/**
	 * Check for illegal overloads of the definitions in the class. Note that
	 * ambiguous overloads between classes in the hierarchy are only detected
	 * when the name is used, and so appears in findName below.
	 */
	private void checkOverloads()
	{
		List<String> done = new Vector<String>();

		TCDefinitionList singles = definitions.singleDefinitions();

		for (TCDefinition def1: singles)
		{
			for (TCDefinition def2: singles)
			{
				if (def1 != def2 &&
					def1.name != null && def2.name != null &&
					def1.name.equals(def2.name) &&
					!done.contains(def1.name.getName()))
				{
					if ((def1.isFunction() && def2.isFunction()) ||
						(def1.isOperation() && def2.isOperation()))
					{
    					TCType to = def1.getType();
    					TCType from = def2.getType();

    					// Note this uses the "parameters only" comparator option

    					if (TypeComparator.compatible(to, from, true))
    					{
    						def1.report(3008, "Overloaded members indistinguishable: " + def1.name.getName());
    						detail2(def1.name.getName(), def1.getType(), def2.name.getName(), def2.getType());
    						done.add(def1.name.getName());
    					}
					}
					else
					{
						// Class invariants can duplicate if there are several
						// "inv" clauses in one class...

						if (!(def1 instanceof TCClassInvariantDefinition) &&
							!(def2 instanceof TCClassInvariantDefinition) &&
							!(def1 instanceof TCPerSyncDefinition) &&
							!(def2 instanceof TCPerSyncDefinition))
						{
    						def1.report(3017, "Duplicate definitions for " + def1.name);
    						detail2(def1.name.getName(), def1.location, def2.name.getName(), def2.location);
    						done.add(def1.name.getName());
						}
					}
				}
			}
		}
	}

	/**
	 * Set superInheritedDefinitions and localInheritedDefinitions.
	 */
	private void setInheritedDefinitions()
	{
		TCDefinitionList indefs = new TCDefinitionList();

		for (TCClassDefinition sclass: superdefs)
		{
			indefs.addAll(sclass.getInheritable());
		}

		// The inherited definitions are ordered such that the
		// definitions, taken in order, will consider the overriding
		// members before others.

		superInheritedDefinitions = new TCDefinitionList();

		for (TCDefinition d: indefs)
		{
			superInheritedDefinitions.add(d);

			TCNameToken localname = d.name.getModifiedName(name.toString());

			if (definitions.findName(localname, NameScope.NAMESANDSTATE) == null ||
				d.isSubclassResponsibility())
			{
				TCInheritedDefinition local = new TCInheritedDefinition(d.accessSpecifier, localname, d);
				localInheritedDefinitions.add(local);
			}
		}

		allInheritedDefinitions =  new TCDefinitionList();
		allInheritedDefinitions.addAll(superInheritedDefinitions);
		allInheritedDefinitions.addAll(localInheritedDefinitions);
	}

	private boolean gettingInheritable = false;

	/**
	 * Get a list of inheritable definitions from all the superclasses. This
	 * is a deep search, including definitions from super-superclasses etc.
	 */
	private TCDefinitionList getInheritable()
	{
		TCDefinitionList defs = new TCDefinitionList();

		if (gettingInheritable)
		{
			report(3009, "Circular class hierarchy detected: " + name);
			return defs;
		}

		gettingInheritable = true;

		// The inherited definitions are ordered such that the
		// definitions, taken in order, will consider the overriding
		// members before others. So we add the local definitions
		// before the inherited ones.

		TCDefinitionList singles = definitions.singleDefinitions();

		for (TCDefinition d: singles)
		{
			if (d.accessSpecifier.access != Token.PRIVATE)
			{
				defs.add(d);
			}
		}

		for (TCClassDefinition sclass: superdefs)
		{
			TCDefinitionList sdefs = sclass.getInheritable();

			for (TCDefinition d: sdefs)
			{
				defs.add(d);

				TCNameToken localname = d.name.getModifiedName(name.toString());

				if (defs.findName(localname, NameScope.NAMESANDSTATE) == null)
				{
					TCInheritedDefinition local = new TCInheritedDefinition(d.accessSpecifier, localname, d);
					defs.add(local);
				}
			}
		}

		gettingInheritable = false;
		return defs;
	}

	/**
	 * Get a list of all definitions for this class, including the local
	 * definitions as well as those inherited.
	 *
	 * @see com.fujitsu.vdmj.ast.definitions.TCDefinition#getDefinitions()
	 */
	@Override
	public TCDefinitionList getDefinitions()
	{
		TCDefinitionList all = new TCDefinitionList();

		all.addAll(allInheritedDefinitions);
		all.addAll(definitions.singleDefinitions());

		return all;
	}

	/**
	 * Get a list of all local definitions for this class. These are the names
	 * which can be accessed locally, without a class qualifier.
	 */
	public TCDefinitionList getLocalDefinitions()
	{
		TCDefinitionList all = new TCDefinitionList();

		all.addAll(localInheritedDefinitions);
		all.addAll(definitions.singleDefinitions());

		return all;
	}

	/**
	 * Generate a local definition for "self".
	 *
	 * @return a LocalDefinition for "self".
	 */
	@Override
	public TCDefinition getSelfDefinition()
	{
		TCDefinition def = new TCLocalDefinition(location, name.getSelfName(), getType());
		def.markUsed();
		return def;
	}

	/**
	 * Get this class' TCClassType.
	 */
	@Override
	public TCType getType()
	{
		if (classtype == null)
		{
			classtype = new TCClassType(location, this);
		}

		return classtype;
	}

	/**
	 * True, if this class has the one passed as a super type. Note that the
	 * class type itself is considered as "true".
	 */
	public boolean hasSupertype(TCType other)
	{
		if (getType().equals(other))
		{
			return true;
		}
		else
		{
			for (TCType type: supertypes)
			{
				TCClassType sclass = (TCClassType)type;

				if (sclass.hasSupertype(other))
				{
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * True, if the field passed can be accessed from the current context.
	 */
	static public boolean isAccessible(Environment env, TCDefinition field, boolean needStatic)
	{
		TCClassDefinition self = env.findClassDefinition();
		TCClassDefinition target = field.classDefinition;

		if (self == null)	// Not called from within a class member
		{
			// We're outside, so just public access
			return (field.accessSpecifier.access == Token.PUBLIC);
		}
		else
		{
			TCClassType selftype = (TCClassType)self.getType();
			TCClassType targtype = (TCClassType)target.getType();

			if (!selftype.equals(targtype))
			{
				if (selftype.hasSupertype(targtype))
				{
					// We're a subclass, so see public or protected
					return (field.accessSpecifier.access != Token.PRIVATE);
				}
				else
				{
					// We're outside, so just public/static access
					return (field.accessSpecifier.access == Token.PUBLIC &&
							(needStatic ? field.accessSpecifier.isStatic : true));
				}
			}
			else
			{
				// else same type, so anything goes
				return true;
			}
		}
	}

	@Override
	public String toString()
	{
		return	"class " + name +
				(supernames.isEmpty() ? "" : " is subclass of " + supernames) + "\n" +
				definitions.toString() +
				"end " + name + "\n";
	}

	/**
	 * Find a definition within this class by name. Note that this includes a
	 * check for ambiguous names that could be resolved in different ways from
	 * different overloaded members.
	 */
	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		TCDefinition def = null;

		for (TCDefinition d: definitions)
		{
			TCDefinition found = d.findName(sought, scope);

			// It is possible to have an ambiguous name if the name has
			// type qualifiers that are a union of types that match several
			// overloaded functions/ops (even though they themselves are
			// distinguishable).

			if (found != null)
			{
				if (def == null)
				{
					def = found;

					if (sought.getTypeQualifier() == null)
					{
						break;		// Can't be ambiguous
					}
				}
				else
				{
					if (!def.location.equals(found.location) &&
						def.isFunctionOrOperation())
					{
						if (findFrom != null)
						{
							findFrom.report(3010, "Name " + sought + " is ambiguous");
						}
						else
						{
							sought.report(3010, "Name " + sought + " is ambiguous");
						}

						detail2("1", def.location, "2", found.location);
						break;
					}
				}
			}
		}

		if (def == null)
		{
			for (TCDefinition d: allInheritedDefinitions)
			{
				TCDefinition indef = d.findName(sought, scope);

				// See above for the following...

				if (indef != null)
				{
					if (def == null)
					{
						def = indef;

						if (sought.getTypeQualifier() == null)
						{
							break;		// Can't be ambiguous
						}
					}
					else if (def.equals(indef) &&	// Compares qualified names
							 !def.location.equals(indef.location) &&
							 !def.classDefinition.hasSupertype(indef.classDefinition.getType()) &&
							 def.isFunctionOrOperation())
					{
						sought.report(3011, "Name " + sought + " is multiply defined in class");
						detail2("1", def.location, "2", indef.location);
						break;
					}
				}
			}
		}

		return def;
	}

	/**
	 * Find a type definition within this class by name. Note that this includes
	 * a check for ambiguous types that could be resolved in different ways from
	 * different supertypes.
	 */
	@Override
	public TCDefinition findType(TCNameToken sought, String fromModule)
	{
		if ((!sought.isExplicit() && sought.getName().equals(name.getName())) ||
			sought.equals(name.getClassName()))
		{
			return this;	// Class referred to as "A" or "CLASS`A"
		}

		TCDefinition def = definitions.findType(sought, null);

		if (def == null)
		{
			for (TCDefinition d: allInheritedDefinitions)
			{
				TCDefinition indef = d.findType(sought, null);

				if (indef != null)
				{
					def = indef;
					break;
				}
			}
		}

		return def;
	}

	/**
	 * Find a list of definitions whose names match that passed (overloads) 
	 */
	public TCDefinitionSet findMatches(TCNameToken name)
	{
		TCDefinitionSet set = definitions.findMatches(name);
		set.addAll(allInheritedDefinitions.findMatches(name));
		return set;
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
	public TCDefinition findConstructor(TCTypeList argtypes, TCExpression  findFrom)
	{
		this.findFrom = findFrom;
		TCNameToken constructor = getCtorName(argtypes);
		TCDefinition d = findName(constructor, NameScope.NAMES);
		findFrom = null;
		return d;
	}

	/**
	 * Find a thread definition for this class.
	 */
	public TCDefinition findThread()
	{
		return findName(name.getThreadName(), NameScope.NAMES);
	}

	/**
	 * Resolve the types in all definitions.
	 */
	@Override
	public void typeResolve(Environment globals)
	{
		Environment cenv = new FlatEnvironment(definitions, globals);
		TypeCheckException problem = null;
		
		for (TCDefinition d: definitions)
		{
			try
			{
				d.typeResolve(cenv);
			}
			catch (TypeCheckException te)
			{
				if (problem == null)
				{
					problem = te;
				}
				else
				{
					problem.addExtra(te);
				}
			}
		}
		
		if (problem != null)
		{
			throw problem;
		}
	}

	/**
	 * Type check all definitions.
	 */
	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		assert false : "Can't call Class definition type check";
	}

	/**
	 * Call typeCheck for the definitions within the class that are of the Pass
	 * type given.
	 */
	public void typeCheckPass(Pass p, Environment base)
	{
		if (p == Pass.TYPES)	//  First pass
		{
			localInheritedDefinitions.removeDuplicates();
			isAbstract = getLocalDefinitions().removeAbstracts().hasSubclassResponsibility();
			getType();	// Just set the classtype
		}

		for (TCDefinition d: definitions)
		{
			if (d.pass == p)
			{
				Environment env = base;
				
				if (d instanceof TCValueDefinition)
				{
					// TCValueDefinition body always a static context
					FlatCheckedEnvironment checked = new FlatCheckedEnvironment(new TCDefinitionList(), base, NameScope.NAMES);
					checked.setStatic(true);
					env = checked;
				}
				
				d.typeCheck(env, NameScope.NAMES);
			}
		}

		if (invariant != null && invariant.pass == p)
		{
			invariant.typeCheck(base, NameScope.NAMES);
		}
	}

	private boolean gettingInvDefs = false;

	/**
	 * Get a list of class invariant operation definitions, for this class and
	 * all of its supertypes.
	 */
	public TCDefinitionList getInvDefs()
	{
		TCDefinitionList invdefs = new TCDefinitionList();

		if (gettingInvDefs)
		{
			// reported elsewhere
			return invdefs;
		}

		gettingInvDefs = true;

		for (TCClassDefinition d: superdefs)
		{
			invdefs.addAll(d.getInvDefs());
		}

		for (TCDefinition d: definitions)
		{
			if (d instanceof TCClassInvariantDefinition)
			{
				invdefs.add(d);
			}
		}

		gettingInvDefs = false;
		return invdefs;
	}

	/**
	 * Generate the class invariant definition for this class, if any.
	 */
	private TCExplicitOperationDefinition getInvDefinition()
	{
		TCDefinitionList invdefs = getInvDefs();

		if (invdefs.isEmpty())
		{
			return null;
		}

		// Location of last local invariant
		LexLocation invloc = invdefs.get(invdefs.size() - 1).location;

		TCOperationType type = new TCOperationType(invloc, new TCTypeList(), new TCBooleanType(invloc));
		type.setPure(true);

		TCNameToken invname = name.getInvName(invloc);
		TCStatement body = new TCClassInvariantStatement(invname, invdefs);
		TCAccessSpecifier access = new TCAccessSpecifier(false, false, Token.PRIVATE, true);

		return new TCExplicitOperationDefinition(null, access, invname, type, new TCPatternList(), null, null, body);
	}

	@Override
	public boolean isTypeDefinition()
	{
		return true;	// A class is regarded as a type
	}

	public void initializedCheck()
	{
		definitions.initializedCheck();
	}

	@Override
	public String kind()
	{
		return "class";
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseClassDefinition(this, arg);
	}
}
