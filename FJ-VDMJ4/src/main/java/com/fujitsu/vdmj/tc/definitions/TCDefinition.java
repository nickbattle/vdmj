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

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.typechecker.TypeComparator;

/**
 * The abstract parent of all definitions. A definition can represent a data
 * type, a value (constant), implicit or explicit functions, implicit or
 * explicit operations, module state, as well as various sorts of local variable
 * definition.
 */
public abstract class TCDefinition extends TCNode implements Serializable, Comparable<TCDefinition>
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the definition */
	public final LexLocation location;
	
	/** The name of the object being defined. */
	public final TCNameToken name;
	
	/** The scope of the definition name. */
	public final NameScope nameScope;
	
	/** The pass to cover this definition in type checking. */
	public Pass pass;

	/** True if the definition has been used by the rest of the code. */
	public boolean used = false;
	
	/** True if the definition should be excluded from name lookups */
	public boolean excluded = false;

	/** A public/private/protected/static specifier, if any. */
	public TCAccessSpecifier accessSpecifier = null;
	
	/** A pointer to the enclosing class definition, if any. */
	public TCClassDefinition classDefinition = null;

	/**
	 * Create a new definition of a particular name and location.
	 */
	public TCDefinition(Pass pass, LexLocation location, TCNameToken name, NameScope scope)
	{
		this.pass = pass;
		this.location = location;
		this.name = name;
		this.nameScope = scope;
		this.accessSpecifier = TCAccessSpecifier.DEFAULT;
	}

	@Override
	abstract public String toString();
	
	abstract public String kind();

	@Override
	public boolean equals(Object other)		// Used for sets of definitions.
	{
		if (other instanceof TCDefinition)
		{
			TCDefinition odef = (TCDefinition)other;
			return name != null && odef.name != null && name.equals(odef.name);
		}

		return false;
	}
	
	@Override
	public int compareTo(TCDefinition o)
	{
		return name == null ? 0 : name.compareTo(o.name); 
	};

	@Override
	public int hashCode()
	{
		return name.hashCode();		// Used for sets of definitions (see equals).
	}

	/**
	 * Perform a static type check of this definition. The actions performed
	 * depend on the type of definition and are entirely defined in the
	 * subclass. The type checker is passed an Environment object which
	 * contains a list of other definitions, and a scope which indicates
	 * what sort of names from the environment can be used (state etc).
	 *
	 * @param base	Named definitions potentially in scope
	 * @param scope The types of names in scope
	 */
	abstract public void typeCheck(Environment base, NameScope scope);

	/**
	 * Resolve the unresolved types in the definition. TCType resolution means
	 * looking up type names (which are all that is known after parsing, unless
	 * the types are primitive) and replacing them with the {@link TCType} value
	 * from the definition of the named type in the {@link Environment} passed.
	 * <p>
	 * This method is defined for PODefinition subclasses which have a
	 * {@link com.fujitsu.vdmj.typechecker.Pass Pass} value of
	 * {@link com.fujitsu.vdmj.typechecker.Pass Pass.TYPES}.
	 *
	 * @param env
	 */
	public void typeResolve(Environment env)
	{
		return;
	}

	/**
	 * Return a list of all the definitions created by this definition. A
	 * definition may create lower level definitions if it defines more than one
	 * "name" which can be used in its scope. For example, a function may define
	 * pre and post conditions, which cause implicit functions to be defined in
	 * addition to the main definition for the function itself.
	 *
	 * @return A list of definitions.
	 */
	abstract public TCDefinitionList getDefinitions();

	/**
	 * Return a list of variable names that would be defined by the definition.
	 */
	abstract public TCNameList getVariableNames();

	/**
	 * Return a list of free variables needed for the definition to initialise.
	 */
	public TCNameSet getFreeVariables()
	{
		return new TCNameSet();
	}
	
	/**
	 * Return the static type of the definition. For example, the type of a
	 * function or operation definition would be its parameter/result signature;
	 * the type of a value definition would be that value's type; the type of a
	 * type definition is the underlying type being defined.
	 * <p>
	 * Note that for Definitions which define multiple inner definitions (see
	 * {@link #getDefinitions}), this method returns the primary type - eg.
	 * the type of a function, not the types of its pre/post definitions.
	 *
	 * @return The primary type of this definition.
	 */
	abstract public TCType getType();

	/**
	 * Complete the generation of implicit definitions for this definition.
	 * Some definition types create implicit definitions, such as pre_ and
	 * post_ functions, inv_ and init_ functions. These definitions can be
	 * referred to by name, so they must be created shortly after the parse
	 * of the explicit definitions.
	 *
	 * @param base The environment defining all global definitions.
	 */
	public void implicitDefinitions(Environment base)
	{
		return;
	}

	/**
	 * Find whether this PODefinition contains a definition of a name. Since some
	 * definitions contain inner definitions (eg. function definitions contain
	 * further definitions for their pre and post conditions), this cannot in
	 * general compare the name passed with the name of this object.
	 *
	 * This method is used for finding Definitions that are not types. That can
	 * include constants, functions, operations and state definitions. The
	 * POTypeDefinition implementation of this method checks for any type
	 * invariant function definition that it has created, but does not return a
	 * match for the name of the type itself. The {@link #findType findType}
	 * method is used for this.
	 *
	 * The implementation in the base class compares the name with this.name,
	 * setting the "used" flag if the names match. Subclasses perform a similar
	 * check on any extra definitions they have created.
	 *
	 * Definitions which include state fields are sometimes in scope (from
	 * within operation bodies) and sometimes not (in function bodies). So the
	 * scope parameter is used to indicate what sorts of definitions should be
	 * considered in the lookup. The {@link POStateDefinition} subclass uses this
	 * to decide whether to consider its state definitions as in scope.
	 *
	 * @param sought The name of the definition sought.
	 * @param scope The sorts of definitions which may be considered.
	 * @return A definition object, or null if not found.
	 */
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		if (name.equals(sought))
		{
			if (!nameScope.matches(scope))
			{
				sought.report(3302,
					"State variable '" + sought.toString() + "' cannot be accessed from this context");
			}

			markUsed();
			return this;	// To prevent searching further, even if scope is wrong
		}

		return null;
	}

	/**
	 * Set the "used" flag.
	 */
	public void markUsed()
	{
		used = true;
	}

	/**
	 * Test the "used" flag.
	 */
	protected boolean isUsed()
	{
		return used;
	}

	/**
	 * Find whether this definition contains a definition of a type name. This
	 * is very similar to {@link #findName findName}, except that there is no
	 * need for a scope parameter since state definitions' types are always in
	 * scope. The method is implemented by the {@link POTypeDefinition} class, by
	 * {@link POStateDefinition} (when module state is referred to as a
	 * record type), and by {@link com.fujitsu.vdmj.ast.modules.ASTImportedType TCImportedType}
	 * definitions.
	 *
	 * @param name The name of the type definition being sought.
	 * @param fromModule The name of the module seeking the type.
	 * @return The type definition or null.
	 */
	public TCDefinition findType(TCNameToken name, String fromModule)
	{
		return null;
	}

	/**
	 * Check whether this definition has ever been used. This method is called
	 * when a definition goes out of scope. If the "used" flag has not been set,
	 * then nothing has referenced the variable during its lifetime and an
	 * "unused variable" warning is given. The "used" flag is set after the
	 * first warning to prevent repeat warnings.
	 */
	public void unusedCheck()
	{
		if (!isUsed())
		{
			warning(5000, "Definition '" + name + "' not used");
			markUsed();		// To avoid multiple warnings
		}
	}

	/**
	 * Set the definition's AccessSpecifier. This is used in VDM++ definitions
	 * to hold  static and public/protected/private settings.
	 * 
	 * TODO: This method is needed, but I can't see why TC needs it?
	 *
	 * @param access The AccessSpecifier to set.
	 */
	public void setAccessSpecifier(TCAccessSpecifier access)
	{
		accessSpecifier = access;
	}

	/**
	 * Test access specifier. An empty specifier defaults to PRIVATE.
	 */
	public boolean isAccess(Token kind)
	{
		switch (kind)
		{
			case STATIC:
				return accessSpecifier.isStatic;

			default:
				return accessSpecifier.access == kind;
		}
	}

	/**
	 * Test for a static access specifier.
	 */
	public boolean isStatic()
	{
		return accessSpecifier.isStatic;
	}
	
	/**
	 * Test whether definition (operation) is pure.
	 */
	public boolean isPure()
	{
		return accessSpecifier.isPure;
	}

	/**
	 * Return true if the definition is of an implicit or explicit function
	 * or operation.
	 */

	public final boolean isFunctionOrOperation()
	{
		return isFunction() || isOperation();
	}

	public boolean isFunction()
	{
		return false;
	}

	public boolean isOperation()
	{
		return false;
	}

	/**
	 * Return true if the definition is an operation that defines a body.
	 */

	public boolean isCallableOperation()
	{
		return false;
	}

	public boolean isCallableFunction()
	{
		return false;
	}

	/**
	 * Return true if the definition is an instance variable.
	 */
	public boolean isInstanceVariable()
	{
		return false;
	}

	/**
	 * Return true if the definition is a type definition.
	 */
	public boolean isTypeDefinition()
	{
		return false;
	}

	/**
	 * Return true if the definition is a value definition.
	 */
	public boolean isValueDefinition()
	{
		return false;
	}

	/**
	 * Return true if the definition generates a value which should be used
	 * at runtime. For example, TypeDefinitions don't.
	 */
	public boolean isRuntime()
	{
		return true;	// Most are!
	}

	/**
	 * Return true if the definition generates a value which is updatable,
	 * like a state value, an instance variable, or a dcl declaration.
	 */
	public boolean isUpdatable()
	{
		return false;	// Most aren't!
	}

	/**
	 * Check for not yet specified or is subclass responsibility. 
	 */
	public boolean isSubclassResponsibility()
	{
		return false;
	}

	/**
	 * Set the associated TCClassDefinition for this definition. This is used with
	 * VDM++ where {@link TCClassDefinition} instances contain definitions for all
	 * the contained functions, operations, types, values and instance variables.
	 *
	 * @param def
	 */
	public void setClassDefinition(TCClassDefinition def)
	{
		classDefinition = def;
	}

	/**
	 * Generate a local definition for "self". Note that this assumes the
	 * setClassDefinition method has been called.
	 *
	 * @return a POLocalDefinition for "self".
	 */
	protected TCDefinition getSelfDefinition()
	{
		return classDefinition.getSelfDefinition();
	}

	/**
	 * Report the message as a type checking error, increment the error count,
	 * and continue.
	 */
	public void report(int number, String msg)
	{
		TypeChecker.report(number, msg, location);
	}

	/**
	 * Report the message as a type checking warning, increment the warning count,
	 * and continue.
	 */
	public void warning(int number, String msg)
	{
		TypeChecker.warning(number, msg, location);
	}

	/**
	 * Add detail to a type checking error or warning. For example, the report
	 * method might be used to indicate that an unexpected type was used, but
	 * the detail method can be used to indicate what the expected type was.
	 *
	 * @param tag	The message.
	 * @param obj	The value associated with the message.
	 */
	public void detail(String tag, Object obj)
	{
		TypeChecker.detail(tag, obj);
	}

	/**
	 * Add detail to a type checking error or warning. As {@link #detail},
	 * except two tag/value pairs can be passed (eg. "Actual" x, "Expected" y).
	 */
	public void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		TypeChecker.detail2(tag1, obj1, tag2, obj2);
	}

	/**
	 * Dereference imported, inherited or renamed definitions.
	 */
	public TCDefinition deref()
	{
		return this;
	}
	
	/**
	 * Check a PODefinitionList for incompatible duplicate pattern definitions.
	 */
	public TCDefinitionList checkDuplicatePatterns(TCDefinitionList defs)
	{
		TCDefinitionSet noDuplicates = new TCDefinitionSet();
		
		for (TCDefinition d1: defs)
		{
			for (TCDefinition d2: defs)
			{
				if (d1 != d2 && d1.name != null && d2.name != null && d1.name.equals(d2.name))
				{
					if (!TypeComparator.compatible(d1.getType(), d2.getType()))
					{
						report(3322, "Duplicate patterns bind to different types");
						detail2(d1.name.toString(), d1.getType(), d2.name.toString(), d2.getType());
					}
				}
			}
			
			noDuplicates.add(d1);
		}

		return noDuplicates.asList();
	}
}
