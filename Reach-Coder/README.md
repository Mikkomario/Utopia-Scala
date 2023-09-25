# Reach Coder
Reach Coder -utility application is used for generating Scala code necessary for constructing **Reach** components. 
It generates the appropriate classes, getters and setters for all defined components and properties, 
saving you a lot of programming time when developing custom components.

This application works with the same foundation and principles as the **Vault Coder** utility application.

## App command syntax
First, you need to prepare a .json file describing the component structure you want to create / use.
See the required document structure below.

Once you have created a specification document, run the application using the following command line command:
`java -jar Reach-Coder.jar <root> <input> <output> <filter> <merge>`
- `<root>` (`project`)
    - Here you define either:
      - The path (beginning) that is common for both the input and the output
      - Previously stored **project name**, in which case you may omit the `input` and `output` values
    - This argument is **optional**
- `<input>` (`in`) points towards your specification .json file or to a directory that holds such files
    - This argument is optional, but if `<root>` is not specified, this value is requested during application use
    - If `<root>` is specified, this path should be relative to it
    - If `<root>` is specified and this value is not specified, uses `<root>` as the input path
- `<output>` (`out`) argument specifies the path to where the generated code will be stored
    - If `<root>` is specified, this path should be relative to it
    - If this argument is not specified, it is requested during application use
    - If the target directory doesn't exist, it will be created
- `<filter>` is a filter that is used to reduce the number of components that are written
    - This argument is optional
    - If `<group>` is specified and this value is not, the application allows the user to type a filter during
      application use
    - This argument is case-insensitive and should represent a portion of the targeted item's name
        - If, however, you include the `-S` flag in command arguments, class names must match the filter exactly
          (except for casing) and not just partially.
- `<merge>` Specifies location of the existing source root (src) directory which is compared and merged into
  the generated output
    - This argument is optional. If this is left empty, merging will only occur for saved projects
    - You may also specify this value during program use if you pass `-M` as a command line argument
    - Alternatively, you may specifically disable merging by passing `-N` as a command line argument
        - This is only necessary on saved projects

The program will inform you if there were any problems during input file processing or during output write operations.

## Input file syntax
A components .json file may contain the following properties:
- `project: String` - Defines the name of the project in question. Affects file-naming.
- `version: String (optional)` - Targeted software version. Affects the generated `@since` -scaladoc
- `author: String (optional)` - Code author. Affects generated `@author` -scaladoc
- `package: Package (optional)` - Root package for the generated components
  - If omitted, the root package must be specified under the `components` -property
- `packages: Object (optional)` - Defines **package aliases**. See the [specific section covering these](#package-aliases).
- `references: Object (optional)` - Defines type aliases. See the [specific section covering these](#type-aliases).
- `components: Object` - Lists written components. See the [specific section covering these](#component-package-structure).

See [input-template.json](https://github.com/Mikkomario/Utopia-Scala/blob/development/Reach-Coder/input-template.json) 
as an example of input file syntax.

### Package aliases
Package aliases are defined by passing a json object to the `package` property. 
Each **key** in the specified object defines a new **alias**, 
and each **String value** defines the **referenced package**. 

Package aliases may refer to other package aliases, as long as cyclic references are avoided. 
Whenever you refer to a package alias, you must suffix it with `/`. 
For example, a package alias `"alias2": "alias1/subpackage.more"`, where alias1 is defined as `"test.input"` 
resolves into `test.input.subpackage.more`.

### Type aliases
Type aliases are defined by passing a json object to the `references` property. 
Each **key** in the specified object defines a new **alias**, 
and each **String value** defines the **referenced class**.

Type aliases may freely use package aliases within their definitions, provided that the `/` suffix is correctly included. 
In cases where package alias is omitted, full package reference should be included, 
as if you were writing an import statement.

Type aliases are referenced simply with their full property name.

### Component package structure
When defining components, you specify the package structure using json objects. 
**Object** values are interpreted to represent **sub-packages**, while **array** values are interpreted to represent 
**component lists** under the package listed in the property name. 
Package (property) names may specify nested packages. E.g. `"pck1.pck2"` resolves into `pck2` under package `pck1`.

E.g. `"package1": { "package2": [...] }` is interpreted as a component list under package `package1.package2`, which is 
also achievable with syntax `"package1.package2": [...]`.

### Individual component object syntax
This section describes the properties that are supported for individual component objects. 
Each object represents a single UI component class within the package in which it is listed 
(see [Component package structure](#component-package-structure)).

The supported properties are:
- `name: String` - Name of this component (in CamelCase)
- `name_plural: String (optional)` - Plural version of this component's name
  - Will be autogenerated if omitted
- `parents: [String] (optional)` - Applied parent traits
  - Only supports pre-built options, which are:
    - `"draw"` for **CustomDrawableFactory**
    - `"focus"` for **FocusListenableFactory**
    - `"frame"` for **FramedFactory**
  - Values which include these keywords are also supported
    - E.g. You may write `"CustomDrawable"` instead of `"draw"`
- `props: [Object]` - Lists component creation properties that may be modified
  - Specify here only those properties that appear in **both contextual and non-contextual** component factory variants
- `contextual_props: [Object]` - Lists component creation properties that only apply to contextual component factories
- `non_contextual_props: [Object]` - Lists component creation properties that 
  only apply to non-contextual component factories
- `only_contextual: Boolean (optional)` - Set to true if you want to omit the non-contextual factory variant
- `variable_context: Boolean (optional)` - Set to true if you want the contextual factory to use context pointers

Please see the [property object syntax](#component-property-syntax) below, 
concerning `props`, `contextual_props` and `non_contextual_props`.

#### Component property syntax
Component properties come in two types: **standard properties** and **references** to other component property sets.

Properties used when defining **standard properties**:
- `name: String` - Name of the defined property / setting
- `name_plural: String (optional)` - Plural version of this property's name
  - This will be autogenerated if omitted
- `setter: String (optional)` - Name of the generated setter function matching this property
  - If omitted, this will be autogenerated by prepending the property name with `with`. 
    - E.g. `name: "prop1"` setter would be named `setProp1`
- `param: String (optional)` - Name used for this property when it appears as a setter parameter
  - If omitted, the `name` value will be used
  - E.g. if `param` is defined as `p` and `setter` is defined as `withProp`, the resulting code will read 
    `withProp(p: ...)`
- `type: Type` - Data type used in this property
  - Here you may either specify the whole type, including the package prefix, or use a **type alias**.
  - When specifying a package prefix, you're allowed to use **package references**, provided they're suffixed with `/`.
- `default: Code (optional)` - A piece of code used as the default value for this property
  - **If** you need to refer to other classes, use object syntax with the following two properties:
    - `code: String` - The code portion, as written in the resulting code
    - `references: [String]` - References made within the code you specified. These will be added as imports.
      - You may freely use **type aliases** and **package aliases** when specifying references.
  - If you don't need any imports, simply write the code as a string
- `mapping: Boolean (optional)` - Whether mapper functions should be generated in addition to setter functions
  - This is false by default
  - Mapper function means a function like `def mapProp1(f: Prop1Type => Prop1Type)`
- `doc: String (optional)` - A description of the function of this property. Passed to generated scaladocs.

In cases where you want to include all properties listed in another component, use the following properties instead:
- `ref: String` - **Name** of the referenced component
- `prefix: String (optional)` - A prefix that should be applied to the referenced setting
  - E.g. If prefix is set to `"image"`, the wrapped setting property will be named `imageSettings`
- `prefix_props: Boolean (optional)` - Whether you want the referenced property getters and setters 
  to include the specified prefix
  - This is **true** by default (provided that prefix is defined)
