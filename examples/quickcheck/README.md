# QuickCheck

This project contains a simple example of VDMJ plugin to quickly check proof obligations.

The plugin registers uses a file of "ranges" to specify the values to give to type binds in a proof obligation.
It then evaluates the PO, which would normally not be executable because of the type binds, using the range
values for each bind. For example, the "test.vdm" spec in the project folder is:

```
	values
		MAX_T = 9;
	
	types
		T = nat
		inv t == t <= MAX_T;
		
	functions
		f: T -> T
		f(a) ==
			if a = 0
			then 1
			else a * f(a-1)
		measure a;
		
		getSet: T * T -> set of T
		getSet(a, b) == {a, ..., b};
```

That creates seven POs:

```
	Proof Obligation 1: (Unproved)
	T: total function obligation in 'DEFAULT' (test.vdm) at line 3:16
	(forall t:nat &
	  is_(inv_T(t), bool))
	
	...
	
	Proof Obligation 7: (Unproved)
	f: subtype obligation in 'DEFAULT' (test.vdm) at line 6:5
	(forall a:T &
	  inv_T((if (a = 0) then 1 else (a * f((a - 1))))) and
	  (is_nat((if (a = 0) then 1 else (a * f((a - 1)))))))
```

A "ranges" file consists of pairs of the form `<multiple bind> = <VDM set-of-T expression>;`. These set values
are used to substitute the type binds in the PO, using the name of the bind to match (so one PO could involve
multiple ranges lines). The set expressions are evaluated in the global environment of the spec, so they can use
types and constants, functions etc, which may help. They can be set enumerations, comprehensions, functions
returning sets and so on. Comments and whitespace are ignored. Each pair is terminated by a semicolon.

```
	-- Simple range of nats
	t : nat = {0, ..., 100};
	
	-- More complex generation, via a function
	a : T = getSet(0, MAX_T);
```

An initial ranges file can be created with the "quickcheck" or "qc" command, using `>filename` as the name.
This will create one line for every bind in the PO list, but the sets will be blank.
After the sets are provided, the "quickcheck" or "qc" command can use the updated range file to exercise POs
with the sets given as the type binds.

```
	> quickcheck
	Usage: quickcheck [>]<ranges file> [<PO numbers>]
	
	> quickcheck >qcfile
	Wrote 2 ranges to qcfile
	>
	
	... edit the sets in qcfile here, then...
	
	> qc qcfile                 -- Try every PO, or provide a list of numbers
		Ranges expanded in 0.049s
		PO# 1, PASSED in 0.018s
		PO# 2, PASSED in 0.001s
		PO# 3, PASSED in 0.001s
		PO# 4, PASSED in 0.0s
		PO# 5, PASSED in 0.002s
		PO# 6, PASSED in 0.003s
		PO# 7, FAILED in 0.008s: Counterexample: a = 4
		f: subtype obligation in 'DEFAULT' (test.vdm) at line 9:5
		(forall a:T &
		  inv_T((if (a = 0) then 1 else (a * f((a - 1))))) and
		  (is_nat((if (a = 0) then 1 else (a * f((a - 1)))))))
	>
```
