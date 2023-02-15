# discharge

This project contains a simple example of VDMJ plugin.

The plugin registers uses a file of "ranges" to specify the values to give to type binds in a proof obligation.
It then evaluates the PO, which would normally not be executable because of the type binds, using the range
values for each bind. For example, the test.vdm spec in the project folder is:

```
functions
	f: int * set of nat -> real
	f(value, group) ==
		if value >= 0
		then value/card group
		else 0;
```

That creates one PO:

```
Proof Obligation 1: (Unproved)
f: non-zero obligation in 'DEFAULT' (test.vdm) at line 5:19
(forall value:int, group:set of (nat) &
  ((value >= 0) =>
    (card group) <> 0))
```

So with a rules file like this:

```
value:int = {-100, ..., 100}
group:set of (nat) = power {1, ..., 10}
```

The discharge command will exercise the PO with the set of ints and sets of nats given.

```
> discharge 1 ranges      -- ie. discharge PO#1 using the ranges file
Result = false            -- Something failed (but we don't know what)
>
```
