# Changes - Utopia Flow
## v1.7 (beta)

### Major Changes
- Module is now based on **Scala 2.13.1** and not 2.12.18

### Breaking Changes
- Loop.runOnce and Loop.nextWaitTarget are now public and not protected. This will cause compile errors in sub-classes.

### New Features
- **SynchronizedLoops** - Added for running multiple loops in a single background thread
- **DailyTask** - A new implementation of **Loop** that is run daily at a specific time
- Period comparison (inexact) (and some other new methods) is now available by importing **TimeExtensions**

### New Methods
- template.TreeLike
    - findWithPath(...)
    - filterWithPaths(...)
- CollectionExtensions
    - Seq
        - splitToSegments(Int)
    - Iterator
        - forNext(Int)(...)