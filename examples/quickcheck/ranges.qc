-- Simple range of nats
t : T! = {0, ..., 100};
t : nat = {0, ..., 100};

-- More complex generation, via a function
a : T = qcGetTRange();
