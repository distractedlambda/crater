# Speculative Variable Homing
## Problem
Lua's semantics preserve the conceptual "identity" of local variables, even in the presence of capturing by closures.
When a variable is assigned to, _all_ lexical references to it observe the assignment, even those that are within
closures. In contrast to many functional languages that yield themselves to trivial "flat" closure implementations,
a Lua implementation cannot blindly copy local values into closure frames and forget their origin.

## Solutions
### Storing Parent `Frame`s
Since all captures must obey lexical scoping rules, a relatively simple solution is to have closures store references to
the frames of their "parent" functions. While this has potential benefits for debugging (allowing a debugger paused
within a closure to see the full frame-state of the parent function), it is not particularly "safe for space". Values
that the closure does not use would still be strongly referenced by the captured parent frame, leading to potential
false retention of heap objects. While this could be mitigated by having the parent function clear out all non-captured
values before returning, that mitigation breaks down in the presence of multiple closures capturing different variables
from the same parent frame.

### Boxing All Captured Variables
A simple "safe for space" solution is to store the value of every potentially-captured variable inside a heap-allocated
box. This is not particularly difficult to implement (in that it is not difficult to statically identify when a local
variable is referenced within closures), and Graal's escape analysis could even eliminate the overhead when the
capturing code-paths are not taken. On the other hand, this approach uses lots of heap memory, and destroys the spatial
locality of accesses to captured values.

### Using Liveness Analysis
It would be possible to perform data-flow analysis on every Lua function to more precisely identify the storage
requirements of local variables. For instance, variables which are never assigned to after being captured could have
copies stored by closures. As a technical downside, this adds to start-up cost, and as a pragmatic downside, it makes 
the "compilation" step much more difficult to implement correctly.

### Speculative Variable Homing
At the start of function execution, local variables are stored directly in `VirtualFrame`s.

When a variable is first captured by a closure, its value at the time of capture is copied into the closure's local
storage, and its value in the `VirtualFrame` is replaced with a special reference to the closure.

When a variable is captured by any additional closures, it is relocated to its own box, with references appropriately
updated.
