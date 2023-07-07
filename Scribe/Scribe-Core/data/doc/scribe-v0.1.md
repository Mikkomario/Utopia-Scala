# Scribe
Version: **v0.1**  
Updated: 2023-07-07

## Table of Contents
- [Enumerations](#enumerations)
  - [Severity](#severity)
- [Packages & Classes](#packages-and-classes)
  - [Logging](#logging)
    - [Error Record](#error-record)
    - [Issue](#issue)
    - [Issue Occurrence](#issue-occurrence)
    - [Issue Variant](#issue-variant)
    - [Stack Trace Element Record](#stack-trace-element-record)

## Enumerations
Below are listed all enumerations introduced in Scribe, in alphabetical order  

### Severity
Represents the level of severity associated with some problem or error situation

Key: `level: Int`  
Default Value: **Unrecoverable**

**Values:**
- **Debug** (1) - An entry used for debugging purposes only. Practically insignificant.
- **Info** (2) - Information about the application's state and/or behavior which may be of use. Doesn't necessarily indicate a real problem.
- **Warning** (3) - Information about the application's state and/or behavior which probably indicates a presence of a problem.
Doesn't necessarily require action.
- **Recoverable** (4) - Indicates a process failure which is either partial or which may possibly be recovered from automatically.
Doesn't require immediate action, but may be important to review and fix eventually.
- **Unrecoverable** (5) - Represents a failure that prematurely terminated some process in a way that progress or data was lost or halted.
Typically the program performance is immediately affected by these kinds of problems.
- **Critical** (6) - Represents a failure that severely or entirely disables the program's intended behavior.
Should be resolved as soon as possible.

Utilized by the following 1 classes:
- [Issue](#issue)

## Packages and Classes
Below are listed all classes introduced in Scribe, grouped by package and in alphabetical order.  
There are a total number of 1 packages and 5 classes

### Logging
This package contains the following 5 classes: [Error Record](#error-record), [Issue](#issue), [Issue Occurrence](#issue-occurrence), [Issue Variant](#issue-variant), [Stack Trace Element Record](#stack-trace-element-record)

#### Error Record
Represents a single error or exception thrown during program runtime

##### Details
- Uses a **combo index**: `exception_type` => `stack_trace_id`

##### Properties
Error Record contains the following 3 properties:
- **Exception Type** - `exceptionType: String` - The name of this exception type. Typically the exception class name.
- **Stack Trace Id** - `stackTraceId: Int` - Id of the topmost stack trace element that corresponds to this error record
  - Refers to [Stack Trace Element Record](#stack-trace-element-record)
- **Cause Id** - `causeId: Option[Int]` - Id of the underlying error that caused this error/failure. None if this error represents the root problem.
  - Refers to [Error Record](#error-record)

##### Referenced from
- [Error Record](#error-record).`causeId`
- [Issue Variant](#issue-variant).`errorId`

#### Issue
Represents a type of problem or an issue that may occur during a program's run

##### Details
- Combines with possibly multiple [Issue Variants](#issue-variant), creating a **Varying Issue**
- **Chronologically** indexed
- Uses a **combo index**: `severity_level` => `context`
- Uses **index**: `created`

##### Properties
Issue contains the following 3 properties:
- **Context** - `context: String` - Program context where this issue occurred or was logged. Should be unique.
- **Severity** - `severity: Severity` - The estimated severity of this issue
- **Created** - `created: Instant` - Time when this issue first occurred or was first recorded

##### Referenced from
- [Issue Variant](#issue-variant).`issueId`

#### Issue Occurrence
Represents one or more specific occurrences of a recorded issue

##### Details
- Uses a **combo index**: `last_occurrence` => `first_occurrence`

##### Properties
Issue Occurrence contains the following 5 properties:
- **Case Id** - `caseId: Int` - Id of the issue variant that occurred
  - Refers to [Issue Variant](#issue-variant)
- **Error Messages** - `errorMessages: Vector[String]` - Error messages listed in the stack trace. 
If multiple occurrences are represented, contains data from the latest occurrence.
- **Details** - `details: Model` - Additional details concerning these issue occurrences.
In case of multiple occurrences, contains only the latest entry for each detail.
- **Count** - `count: Int`, `1` by default - Number of issue occurrences represented by this entry
- **Occurrence Period** - `occurrencePeriod: Span[Instant]`, `Span.singleValue[Instant](Now)` by default - The first and last time this set of issues occurred

#### Issue Variant
Represents a specific setting where a problem or an issue occurred

##### Details
- Combines with [Issue](#issue), creating a **Contextual Issue Variant**
- Combines with multiple [Issue Occurrences](#issue-occurrence), creating a **Issue Variant Instances**
- **Chronologically** indexed
- Uses a **combo index**: `issue_id` => `version` => `error_id`
- Uses **index**: `created`

##### Properties
Issue Variant contains the following 5 properties:
- **Issue Id** - `issueId: Int` - Id of the issue that occurred
  - Refers to [Issue](#issue)
- **Version** - `version: Version` - The program version in which this issue (variant) occurred
- **Error Id** - `errorId: Option[Int]` - Id of the error / exception that is associated with this issue (variant). None if not applicable.
  - Refers to [Error Record](#error-record)
- **Details** - `details: Model` - Details about this case and/or setting.
- **Created** - `created: Instant` - Time when this case or variant was first encountered

##### Referenced from
- [Issue Occurrence](#issue-occurrence).`caseId`

#### Stack Trace Element Record
Represents a single error stack trace line.
A stack trace indicates how an error propagated through the program flow before it was recorded.

##### Details
- Uses a **combo index**: `class_name` => `method_name` => `line_number`

##### Properties
Stack Trace Element Record contains the following 4 properties:
- **Class Name** - `className: String` - The class where this event was recorded.
- **Method Name** - `methodName: String` - The name of the class method where this event was recorded
- **Line Number** - `lineNumber: Int` - The code line number where this event was recorded
- **Cause Id** - `causeId: Option[Int]` - Id of the stack trace element that originated this element. I.e. the element directly before this element. None if this is the root element.
  - Refers to [Stack Trace Element Record](#stack-trace-element-record)

##### Referenced from
- [Error Record](#error-record).`stackTraceId`
- [Stack Trace Element Record](#stack-trace-element-record).`causeId`
