# Utopia Vault Coder
This application allows you to automatically write the basic SQL and Scala code to support your model structure. 
Instead of writing the code manually, you only need to specify a .json file which describes the content to create.

In this document, we will first cover the main use-case, where one manually writes an input json file and then 
converts it to a class structure.  
At the end of this document, you will also find instructions on how to generate input model templates based on 
existing database structure.

## Main App Use Instructions
First, you need to prepare a .json file describing the model structure you want to create / use. 
See the required document structure below.  

Once you have created a specification document, run the application using the following command line command: 
`java -jar Vault-Coder.jar <root> <input> <output> <filter> <group> <merge>`
- `<root>` (`project`) argument specifies **either** the path (beginning) that is common for both the input and the output
  - This argument is optional
  - You may also pass a previously stored **project name** here, in which case you may omit `input` and `output` 
- `<input>` (`in`) points towards your specification .json file or to a directory that holds such files
  - This argument is optional, but if `<root>` is not specified, this value is requested during application use
  - If `<root>` is specified, this path should be relative to it
  - If `<root>` is specified and this is not specified, uses `<root>` as the input path
- `<output>` (`out`) argument specifies the path to where the generated code will be stored 
  - If `<root>` is specified, this path should be relative to it
  - If this argument is not specified, it is requested during application use
  - If the target directory doesn't exist, it will be created
- `<filter>` is a filter that is used to reduce the number of classes or enumerations that are written
  - This argument is optional
  - If `<group>` is specified and this value is not, the application allows the user to type a filter during 
    application use
  - This argument is case-insensitive and should represent a portion of the targeted item's name
    - If, however, you include the `-S` flag in command arguments, class names must match the filter exactly
      (except for casing) and not just partially.
- `<group>` Specifies, which sub-category of items will be written, or which target is filtered using the `<filter>`
  - This argument is optional
  - Allowed values are `class`, `enum`, `package` and `all`
  - If `<filter>` is specified and this value is not, the application asks for this value from the user
  - You can use `-A` flag to specify group `all`
- `<merge>` Specifies location of the existing source root (src) directory which is compared and merged into 
  the generated output
  - This argument is optional. If this is left empty, merging will only occur for saved projects
  - You may also specify this value during program use if you pass `-M` as a command line argument
  - Alternatively, you may specifically disable merging by passing `-N` as a command line argument
    - This is only necessary on saved projects
  - NB: When merging data in projects where the database and the model implementations are separated to different 
    modules, the application will request another (optional) source directory for the other module
- Additionally, if you specify the `-NC` flag, you may prevent any combo classes from being written

The program will inform you if there were any problems during input file processing or during output write operations.

## Input File Structure
This section instructs you on how to write a correct input json document and shows you what options you have 
available to you.  
When writing a new input document, you may start with the 
[input template](https://github.com/Mikkomario/Utopia-Scala/blob/development/Vault-Coder/input-template.json) file.

The input .json file should contain a single object with following properties:
- **"name" / "project": String (optional)** - Name of this project
  - If left empty, the name will be based on the specified base package or database package
  - If your project contains multiple modules, name the Vault-dependent module
- **"author": String (optional)** - Author of this project's model structure
- **"base_package" / "package": String (optional)** - Contains the package prefix common to all generated files 
  (e.g. `"utopia.vault.coder"`)
- **"model_package": String (optional)** - Package where all the model classes are written
  - Defaults to base_package.model
- **"database_package" / "db_package": String (optional)** - Package where all the database -related classes are written
  - Defaults to base_package.database
- **"database_name" / "db_name" / "database" / "db": String (optional)** - Name of the target database
- **"models_without_vault": Boolean (optional)** - Whether model classes can't contain database references 
  (Metropolis-style project) (default = `false`)
- **"prefix_columns": Boolean (optional)** - Whether sql column names should have a table-name -based prefix 
  (default = `false`)
- **"naming": Object (optional)** - An object where you can specify custom naming schemes for the generated documents
  - See [Naming Object Structure](#naming-object-structure) for more details
- **"types" / "data_types": Object (optional)** - Contains a custom data type object for each key. 
  The keys may be used within the property declarations to reference these data types.
  - See [Data Type Object Structure](#data-type-object-structure) for more details
- **"enumerations" / "enums": [Object] (optional)** - List of enumerations introduced within this project. 
  See [Enumeration Object Structure](#enumeration-object-structure) for more details.
  - Alternatively you may pass an object with a property for each introduced enumeration. 
    Each property should have a string array value where each item represents a possible enumeration value.
- **"referenced_enumerations" / "referenced_enums": [String]** - Paths to enumerations from other projects
  - Enumeration paths should include the full reference: E.g. `"utopia.vault.coder.model.datatype.PropertyType"`
- **"classes" / "class": Object** - Contains a property for each introduced sub-package, the value of each 
  sub-package property should be a class object or an array of **class objects** (see [structure](#class-object-structure) below).

### Enumeration Object Structure
Enumeration objects should contain the following properties:
- **"name": String** - Name of this enumeration
- **"id_name" / "id": String (optional)** - The name of the id property of this enumeration's values. 
  The default is `"id"`.
- **"id_type" / "type": String (optional)** - The data type used for the id of this enumeration
  - Please refer to [Property Object Structure](#property-object-structure) for a list of supported data types
  - The default type is **Int**
  - Please note that multi-column data types are properly supported in this context at this time
- **"values": [Object]** - An array that contains the values of this enumeration. 
  - See [Value Object Structure](#enumeration-value-object-structure) for details concerning these objects
  - Alternatively, you may provide an array of strings, where each string is interpreted as an enumeration value name
- **"default": String (optional)** - Name of the default value of this enumeration, if applicable
- **"package": String (optional)** - The package where this enumeration is placed. The default is 
  "project"`.model.enumeration`, where "project" is populated by the project base package.
- **"doc": String (optional)** - Documentation for this enumeration

#### Enumeration Value Object Structure
When specifying enumeration values, please specify the following properties:
- **"name": String** - Name of this value
- **"id" / "key": Code (optional)** - A custom id for this enumeration value. Should yield a value of a correct type.
  - If omitted, the **1-based** index of this value will be used instead
  - Please note that in order to pass a String id, you need to wrap it in double quotes, 
    for example `"\"TestId\""`, which would show as `"TestId"` in the code.
- **"doc": String (optional)** - Documentation for this enumeration value

### Class Object Structure
Class objects should contain following properties:
- **"name": String (optional)** - Name of this class (e.g. `"TestClass"`)
  - If not specified, the name is parsed from the specified table name
- **"name_plural": String (optional)** - Plural version of the class name (e.g. `"TestClasses"`). 
  By default, `"s"` is added to the end of the `"name"` property in order to form the plural name.
- **"table_name" / "table": String (optional)** - Name of the SQL table generated for this class (e.g. `"test_class"`)
  - If not specified, the class name will be parsed into a table name
  - Either this or `"name"` must be provided
- **"id": String (optional)** - Name of this class' id (primary key) property
  - Default is `"id"`
- **"use_long_id": Boolean (optional)** - Whether Long should be used in the id property instead of `Int`. 
  Defaults to `false`.
- **"access_package" / "sub_access" / "access": String (optional)** - Custom sub-package name to use 
  when writing access classes
  - If left empty, the name will be auto-generated based on the class name and the package name
- **"doc": String (optional)** - A description of this class, which is written to class documentation
- **"author": String (optional)** - Author of this class. If omitted, the project author will be used.
- **"combinations" / "combos": [Object]** - An array of **combination objects** for this class 
  (see [structure](#combination-object-structure) below)
  - Alternatively, you can specify a **"combination"** or **"combo"** -property with only a single 
    **combination object** as a value. Please don't specify both, however. 
- **"has_combos" / "generic_access" / "tree_inheritance": Boolean (optional)** - Determines whether a more generic 
  access point (**ManyXAccessLike**) will be written for this class
  - If omitted or *null*, a more generic access point will be written if this declaration includes any combo classes 
- **"description_link" / "desc_link": String (optional)** - Specifies the name of 
  the property in a description link table that refers to this class (e.g. `"testClassId"`)
  - Please note that if you specify this value, localized description support is added for this class
    - This requires the implementing project to use **Utopia Citadel**
- **described: Boolean (optional)** - Determines whether this class should have localized description support, 
  which is a **Utopia Citadel** -specific feature
  - Defaults to `false`
  - This value is ignored if `"description_link_column"` is specified
  - If set to `true`, `"description_link"` value will be autogenerated based on the name of this class
- **"properties" / "props": [Object]** - An array of **property objects** for this class (see structure below)
- **"combo_indices" / "indices": [[String]]** - An array of multiple key indices, each written as an array of property names
  - Alternatively, you can provide `combo_index` or `index` property that contains a single multiple 
    key index array
- **"instances": [Object]** - An array of **instance objects** for this class
  - Instance objects may contain an optional `id` property, which specifies the database row id. 
  - Other properties should be based on class property names.
    - E.g. if the class contains a property with name "testProperty", you may specify a 
      "testProperty" or "test_property" -property in the instance object.
    - Instance objects should specify at least all properties which don't have a sql default value and can't be null.
  - Alternatively, you may provide `instance` property that contains a single instance object. 
    - This will, however, overwrite any value of the `instances` property

#### Property Object Structure
The following guide assumes a standard single-column data type. For multi-column (i.e. combined) data types, 
see the additional instructions under [multi-column properties](#multi-column-properties).

Each property object should contain following properties:
- `"name": String (optional)` - Name of this property (e.g. `"testProperty"`)
  - If not specified, column name will be parsed into a property name. 
    Alternatively the name is based on the data type used. 
    See data type list below for the default names.
- `"name_plural": String (optional)` - The plural form of this property's name (e.g. `"testProperties"`). 
  By default, `"s"` is added to the end of the standard property name in order to form the plural form.
- `"references" / "ref": String (optional)` - Name of the database table referenced by this property.
  - Omit or leave empty if this property doesn't reference any other class.
  - You may specify the referenced column / property in parentheses after the table name. E.g. `"table_name(column_name)"`
    - If column prefixing is used, a prefix will automatically be added to the referenced column name
- `"type": String (optional)` - The data type of this property. The following options are accepted:
  - `"text" / "String" / "varchar"` - Results in a **String** / Varchar type (default name: `"text"`)
    - You may specify the maximum string length in either parentheses at the end of the type (e.g. `"String(32)"`) or 
      via the `"length"` property (optional feature).
      - Additionally, you may specify a flexible limit by separating the initial and the maximum limit with `-`
    - These strings are expected to be valid when empty. They default to an empty string, 
      unless another default value is specified.
  - `"nonEmptyString" / "requiredString" / "StringNotEmpty" / "textNotEmpty"` - 
    Behaves like **String**, but doesn't allow empty values.
  - `"Int"` - Results in **Int** numeric type (default name: `"index"`)
    - You may specify the maximum allowed value in either parentheses at the end of the type 
      (e.g. `"Int(tiny)"` OR `"Int(100)"`) (optional feature)
      - This also supports a flexible limit that is expanded as necessary (e.g. `"Int(20-40000)"`)
  - `"Long" / "bigint" / "number"` - Results in **Long** numeric type (default name: `"number"`)
  - `"Double"` - Results in **Double** numeric type (default name: `"amount"`)
  - `"Boolean" / "flag"` - Results in **Boolean** type (true / false) (default name: `"flag"`)
  - `"datetime" / "timestamp" / "Instant"` - Results in **Instant** / Datetime type (default name: `"timestamp"`)
  - `"date" / "LocalDate"` - Results in **LocalDate** / Date type (default name: `"date"`)
  - `"time" / "LocalTime"` - Results in **LocalTime** / Time type (default name: `"time"`)
  - `"Days"` - Results in **Days** type (default name: `"duration"`)
  - `"DateRange" / "dates"` - Results in **DateRange** data type
  - `"Duration[X]"` - Results in a `FiniteDuration` type (default name: `"duration"`)
    - `X` determines the unit used when storing the duration to the database. Available options are:
      - `h / hour / hours` - Stores as hours (int)
      - `m / min / minute / minutes` - Stores as minutes (int)
      - `s / second / seconds` - Stores as seconds (int)
      - `ms` - Stores as milliseconds (bigint, default)
  - `"creation" / "created"` - Results in **Instant** / Timestamp type and is used as the row creation time 
    (default name: `"created"`)
    - Enables creation time -based indexing (if indexed)
    - Indexes by default
  - `"updated"` - Results in **Instant** / Timestamp type that matches the last time a database row is modified. 
    Similar to `"created"`, except more suitable for mutable items.
    - Enables time-based indexing (if indexed)
    - Indexed by default
  - `"deprecation" / "deprecated"` - Results in **Instant** / Timestamp type that is null / None while the item is 
    active (default name: `"deprecatedAfter"`)
    - Enables item deprecation
    - Indexes by default
  - `"expiration" / "expires"` - Results in **Instant** / Timestamp type that represents item expiration time 
    (default name: `"expires"`)
    - Enables (timed) item deprecation
    - Indexes by default
  - `"value" / "val"` - Results in a generic **Value** type, which are stored in the database as json strings
  - `"model" / "values"` - Results in a **Model** type, which are stored in the database as json strings
  - `"angle"` - Results in **Angle** type from **Paradigm**
  - `"Option[X]"` - Results in **Option** / nullable type where the underlying type is determined by what `X` is
  - `"Vector[X]"` - Results in **Vector** type where the underlying type is determined by what `X` is
    - Vectors are store in the database as json strings
  - `"Vector2D"` - Results in a **Vector2D** class from the **Paradigm** module, not to be confused with **Vector**
    - These are stored in the database as two Double values
  - `"Pair[X]"` - Results in **Pair** type (i.e. two values of same type) where the underlying type is determined by what `X` is
  - `"Span[X]"` - Results in **Span** type that represents a range between two inclusive values
    - `X` determines the span endpoint type
    - If `X` is `Int`, `Double` or `Long`, uses **NumericSpan**
  - `"Distance[X]"` - Results in a **Distance** type from the **Paradigm** module, where `X` represents the unit used 
    when representing the distance as a value.
    - Valid values of `X` are:
      - `Meter` | `m` => Meters
      - `Centimeter` | `cm` => Centimeters
      - `Millimeter` | `millis` | `mm` => Millimeters
      - `Kilometer` | `km` => Kilometers
      - `Feet` | `ft` => Feets
      - `Inch` | `in` => Inches
      - `NauticalMiles` | `NM` => Nautical miles
  - `"LatLong"` - Results in **LatLong** type from the **Terra** module. Uses two separate columns.
  - `"Enum[X]"` - Results in an enumeration value choice. Replace `X` with the enumeration of your choice 
    (must be specified in the `"enumerations"` -property). Uses the enumeration's name as the default property name.
    - Can be wrapped in option
  - **Any custom data type alias** - Results in that custom data type being used
  - If omitted or empty, defaults to `Int`, `String` if the `"length"` property is present
    - Alternatively, the parser may interpret the property type based on the property name
- `"index" / "indexed": Boolean (optional)` - Set to true if you want to force indexing and false if you want 
  to disable it
  - Omit or leave as null if you want the indexing to be determined by the data type
  - References will always create an index, regardless of this value
- `"length": Int (optional)` - Determines maximum text length if `String` type is used
  - Also applies to `Int` type, in which case this property limits the number of digits in an integer
- `"length_rule" / "limit" / "max_length" / "max": String / Int (optional)` - Rule to apply to situations where 
  the current column maximum length would be exceeded.
  - Available options are:
    - `"throw"` - Throws a runtime error
    - `"crop"` - Limits the input so that it fits to the column maximum length
    - `"expand"` - Expands the column maximum length indefinitely
    - `"expand to X"` / `"to X"` - Expands the column maximum length until the specified limit `X`
    - `X` - Expands the column maximum length until the specified limit `X`
  - These are only applicable to `String` and `Int` types
- `"allow_crop" / "crop": Boolean (optional)` - Whether "or crop" should be appended to the specified length rule. 
  Default = false.
  - When true, this will make it so that the column is expanded to certain length, 
    but the input will be cropped if the length would exceed the specified limit.
- `"default" / "def": Code (optional)` - The default value assigned for this property in the data model
  - See [Code Object Structure](#code-object-structure) for specifics
  - If empty or omitted, data type -specific default will be used, if there is one
- `"sql_default" / "sql_def": String (optional)` - The default value assigned for this property in the database
  - If empty or omitted, data type -specific default will be used, if there is one. 
    - A specified default (code) value may also be used, provided it doesn't use any references and is a simple value 
      (such as a string literal, integer or a boolean value)
- `"doc": String (optional)` - Description of this property (used in documentation)

##### Multi-Column Properties
There are certain situations where you want to use a data type that's represented using two or more columns within the 
database. In such a situation, you need to apply the following changes when writing a property:
- Use a custom multi-column data type
  - Currently, there are no inbuilt multi-column data types
- Specify the **"parts"** -property
  - **"parts"** -property should contain a **json object -array** where each object may contain **zero or more** of 
    following properties:
    - **"name": String, "name_plural": String and "column": String** - Like in the main property object, you may use these 
      properties to specify a custom name for each part of the resulting property. While this is optional, 
      it is **highly recommended**.
      - The same rules that apply to the main object apply here. For example, the missing properties may be filled 
        with a value in any of these properties.
    - **"index": Boolean, "length_rule": String / Int, "sql_default": String** - These behave exactly as described above, 
      except that they only apply to this specific column.
- **Don't** include the **"index", "length_rule" or "sql_default"** -properties within the main property object

#### Combination Object Structure
Each combination object should contains following properties:
- **"child" / "children": String** - Name of the linked child class
  - NB: If you use the name `"children"` and don't otherwise specify combination type, 1-n -linking will be used.
    For `"child"` property name, combination type 1- 0-1 will be used instead.
- **"type": String (optional)** - Type of this combination, from one of the options below:
  - **"one" / "link"** => Uses 1 to 1 linking (child must always be present)
  - **"option" / "optional"** => Uses 1 to 0-1 -linking where there can be cases without a child
  - **"multi" / "many"** => Uses 1-n -linking
    - If you use this combination type, you may specify `"is_always_linked": Boolean` -property to specify how to treat
      the 0 children case (default = not always linked => 0 children case is valid)
  - If you omit this property, the combination type is interpreted based on child property name,
    the referred child format and the `"is_always_linked"` property.
- **"is_always_linked" / "always_linked": Boolean (optional)** - Applies to 1-n links, determining whether to always
  expect there to be children in queries
- **"name": String (optional)** - Name of the combined class
  - If omitted, "ParentWithChild" (or "ParentWithChildren") -type of naming will be used
- **name_plural: String (optional)** - Plural form of this class' name
  - If omitted, the singular name is auto-pluralized
- **"parent_alias" / "alias_parent": String (optional)** - Name to use for the parent class within this
  combination class
  - Also affects combination naming if `"name"` is not specified
- **"child_alias" / "alias_child": String (optional)** - Name to use for the child / children in the combined class
  - Also affects combination naming if `"name"` is not specified
- **"child_alias_plural / children_alias"** - Plural form of the child alias
  - If omitted, auto-pluralization is used
- **"doc": String (optional)** - A description of this combined model (used in documentation)

### Code Object Structure
There are two ways to write Scala-code within the input document:
1. By passing a string
2. By passing a json object

When you pass the code as a string, no additional import statements will be added to the resulting code. 
This works for simple use-cases, such as those where you pass an integer or a boolean value.

When you need to use imports, you must pass a json object with following properties:
- **"code": String** - The resulting piece of code
- **"references": [String]** - A json array of the references made within the code. 
  The references should include the full class path of the referred items.
  - Alternatively you may specify **"reference"** -property with just a single reference as a string

### Data Type Object Structure
The following section assumes you're creating a data type that matches to a single database column. If you're 
creating a multi-column data type, please check the [Multi-Column Data Types](#multi-column-data-types) -section as well.

When specifying a custom data type object, you need to specify the following properties:
- **"type": String** - A reference to this data type, including the full class-path as well as possible type parameters.
- **"sql": String** - An sql representation of this data type. For example, `"VARCHAR(32)"`. 
  - This value shouldn't include any `NOT NULL` or `DEFAULT` -statements.
- **"from_value": Code** - Code that accepts a **Value**, which should appear as `$v` within the code, 
  and returns an instance of this type.
  - For example, for Scala **Int** type, this would be `"$v.getInt"`
  - If fromValue-conversion is not guaranteed to succeed, this code may return a **Try**. In this case, 
    remember to specify the `from_value_can_fail` -property.
- **"option_from_value": Code** - Code that accepts a **Value** (`$v`) and returns an **Option** possibly containing an 
  instance of this type
  - For example, for Scala **Int** type, this would be `$v.int`
- **"to_value": Code** - Code that accepts an instance of this type and yields a **Value**
  - The instance parameter should be referenced with `$v` 
  - For example, for Scala **Int** type, this would be 
    `{ "code": "$v", "reference": "utopia.flow.generic.ValueConversions._" }`
- **"option_to_value": Code (optional)** - Code that takes an option that may contain an instance of this type and 
  converts it to a value
  - If omitted or null, `$v.map { v => `to_value`(v) }.getOrElse(Value.empty)` will be used
- **"to_json_value": Code (optional)** - Similar to` to_value`, 
  except that this variant is used when creating json models.
  - If omitted or null, `to_value` will be used instead.
- **"option_to_json_value": Code (optional)** - Similar to `option_to_value`, 
  except that this variant is used when creating json models.
  - If omitted or null, `option_to_value` will be used instead
- **"from_json_value": Code (optional)** - Similar to `from_value`, 
  except that this variant is used when parsing json-originated values.
  - If omitted or null, `from_value` will be used instead.
- **"option_from_json_value": Code (optional)** - Similar to `option_from_value`, 
  except that this variant is used when parsing json-originated values.
  - If omitted or null, `option_from_value` will be used instead
- **"empty" / "empty_value": Code (optional)** - An "empty" instance of this type. Omit if not applicable.
- **"default" / "default_value": Code (optional)** - Default Scala-value for properties using this data type. 
  Omit if not applicable or if same as **"empty"**.
- **"sql_default": String (optional)** - Default value for this data type within sql-documents
- **"prop_name" / "default_name": String (optional)** - Default property name generated for properties using this data 
  type (when they don't specify a name)
  - By default, the name of this data type is used
- **"column_suffix" / "col_suffix" / "suffix": String (optional)** - A custom suffix applied to generated sql property 
  names. Should not include a possible separator prefix, such as `_`
- **"index" / "is_index": Boolean** - Whether this data type creates a database index by default (default = false)
- **"from_value_can_fail" / "yields_try" / "try": Boolean (optional)** - Whether the code passed to `from_value` yields 
  an instance of **Try** instead of an instance of this type. I.e. Whether fromValue-conversion can fail. 
  (default = false)
- **"from_json_value_can_fail" / "json_yields_try" / "json_try": Boolean (optional)** - Similar to `from_value_can_fail`, 
  except that this setting applies to `from_json_value` function.
  - If omitted or null, `from_value_can_fail` will be used.

#### Multi-Column Data Types
In the following examples, we're using an imaginary example data type **Weight** that consists of two parts:
1. The numeric weight amount: `amount: Double`
2. The unit in which the weight amount was given, such as kilograms or pounds, which is represented by some custom 
  enumeration. I.e. `unit: example.enumerations.WeightUnit`. This is stored in the database as a simple integer matching 
  an enumeration's `.id`-property.

When specifying a data type that is represented using two or more database columns, you must also do the following:
- Specify the **"parts"** -property
  - You must pass a **json object array** where each object contains the following properties:
    - **"type": String** - The **Scala** type of this part of the parent type **as it appears in the database model**, 
      i.e. in "optional" form.
      - In our example, these would be `"Option[Double]"` and `"Option[example.enumerations.WeightUnit]"`
    - "sql": String** - The data type listed in the SQL document
      - In our example, these would be `"DOUBLE"` and `"TINYINT"`
    - **"extract": Code** - Code that takes an instance of the full data type and returns an instance of this part's 
      type (in the form listed in the **"type"** property)
      - In our example, these would be `"Some($v.amount)"` and `"Some($v.unit)"`
    - **"extract_from_option": Code** - Code that's used in situations where the original data type is wrapped in **Option**.
      - In our example, these would be `"$v.map { _.amount }"` and `"$v.map { _.unit }"`
    - **"to_value": Code (optional)** - Code that takes an instance of this part 
      (in the database model state, i.e. optional state) and converts it to a **Value**
      - If omitted, a direct value conversion is assumed (using `utopia.flow.generic.ValueConversions`)
      - In our example, the first value would be omitted and the second would read `"$v.map { _.id }"` 
        (and import `utopia.flow.generic.ValueConversions`)
    - **"optional" / "nullable" / "allows_null": Boolean (optional)** - Whether this part allows a NULL value in the 
      database (i.e. is optional)
      - False by default
    - **"empty" / "empty_value": Code (optional)** - The empty or "none" state of this part's "optional" state
      - Default is "None"
    - **"sql_default", "column_suffix" and "index"** as they appear above (all are optional), except that they now only 
      apply to this part / column
- Specify **"from_value"** and **"option_from_value"** -properties in a way that **accepts multiple values** 
  (one for each part). The values should be referenced with `$v1`, `$v2`, `$v3` and so on.
  - In our example, these would read `"WeightUnit.forId($v2.getInt).map { unit => Weight($v1.getDouble, unit) }"` 
    (which is a slightly complex implementation due to the use of enumerations) and 
    `"$v2.int.flatMap(WeightUnit.findForId).flatMap { unit => $v1.double.map { Weight(_, unit) } }"`, both of which would also 
    be modified to include the appropriate references
- **Don't** specify the following properties in the main data type object: 
  **"sql", "sql_default", "column_suffix" and "index"**

### Naming object structure
If you want to specify custom naming schemes, you may do so in a naming object.

Naming rules are interpreted as follows:
- `"camel"` or `"camelCase"` => camel case property naming. E.g. "wordOfLife"
- `"Camel"`, `"CamelCase"`, `"PascalCase"` or `"Pascal"` => pascal case class naming. E.g. "WordOfLife"
- `"underscore"` => lower case words separated by an underscore. E.g. "word_of_life"
- `"kebab"`, `"hyphen"`, `"hyphenated"` or `"dash"` => lower case words separated by a hyphen. E.g. "word-of-life"
- `"text"` => lower case words separated with a whitespace. E.g. "word of life"
- `"Text"` => capitalized words separated with a whitespace. E.g. "Word Of Life"

Below are listed the properties which you may customize:
- `"sql"` => All sql code
  - Default is `"underscore"`
- `"database_name"` / `"database"` / `"db_name"` / `"db"` => database name
  - Default is `"underscore"`, or the value defined for sql
- `"table"` => sql table naming
  - Default is `"underscore"`, or the value defined for sql
- `"column"` / `"col"` => sql column naming
  - Default is `"underscore"`, or the value defined for sql
- `"property"` / `"prop"` => property and parameter names in Scala code
  - Default is `"camelCase"`
  - Changing this is not recommended
- `"db_prop"` / `"db_model_prop"` / `"db_model"` => database model & factory string literals
  - Default is `"camelCase"`, or the value defined for class properties
- `"json"` / `"json_prop"` => generated json object properties
  - Default is `"camelCase"`
- `"documentation"` / `"doc"` / `"text"` => Naming within documentation
  - Default is `"text"`
- `"header"` / `"title"` => Documentation header naming
  - Default is `"Text`", or the value specified for `"documentation"`
- `"file_name"` / `"file"` => Naming within file names
  - Default is `"hyphen"`
- `"class"` => class & trait names
  - Default is `"CamelCase"`
  - Changing this is not recommended
- `"enum"` / `"enumeration"` => Enumeration trait & object names
  - Default is `"CamelCase"`, or the value defined for classes
- `"enum_value"` / `"enum_val"` => Enumeration value object names
  - Default is `"CamelCase"`, or the value defined for enumerations


Please note that if you specify a generic property such as `"sql"`, the rule may be applied to multiple targets, 
unless overridden by a more specific property elsewhere.

Please also note that not all styles are suitable for all use cases.
E.g. Using `"text"` style within code will result in build errors.

## Output Format
This application will produce the following documents 
(where **X** is replaced by class name, **P** by class-related package name and **S** is replaced with project name):
- **S-database-structure.sql** -document that contains create table sql statements
- **S-length-rules.json** -document that contains all listed column length rules
  - Use `ColumnLengthRules.loadFrom(...)` in **Vault** in your code to apply this file to your project
- **S.md** - Class & enumeration documentation
- **S-merge-conflicts-yyyy-mm-dd-hh-mm.txt** -document that lists merge conflicts that occurred (if there were any)
- project root package
  - model
    - combined
      - **P**
        - **DescribedX.scala** - A version of the class which includes descriptions 
        (only for classes with description support)
    - enumeration
      - **E.scala** where **E** goes through each introduced enumeration
    - partial
      - **P**
        - **XData.scala** - The data model containing basic model information
    - stored
      - **P**
        - **X.scala** - A stored variant of the class model
  - database
    - **STables.scala** - An object containing a reference to all tables listed in the **database_structure.sql** document
    - factory
      - **SDescriptionLinkFactory.scala** - An object that contains description link factories for described classes
      - **P**
        - **XFactory.scala** - A factory object used for reading models from database
          - These are also generated for various class combinations
    - model
      - **SDescriptionLinkModel.scala** - An object that contains description link model factories for described classes
      - **P**
        - **XModel.scala** - Database interaction model class + the associated companion object used for forming queries etc.
    - access
      - single
        - **P**
          - **X**
            - **UniqueXAccessLike.scala** - A trait common to distinct single access points that return instances 
              of **X** or any combinations where **X** acts as the parent class
              - Only generated for classes that use combinations
            - **UniqueXAccess.scala** - A trait common to distinct single access points that return instances of **X**
            - **DbSingleX.scala** - A class that accesses individual instances of **X** based on their id
              - Also generated for various class combinations
            - **DbX.scala** - The root access point for individual instances of **X**
              - Also generated for various class combinations
        - description
          - **DbXDescription.scala** - The root access point for individual descriptions targeting instances of **X**
            - Only generated for classes which support descriptions
      - many
        - **P**
          - **X**
            - **ManyXsAccessLike.scala** - A trait common to access points that return multiple instances 
              of **X** or its variations (combinations) at a time
              - Only generated for classes that use combinations
            - **ManyXsAccess.scala** - A trait common to access points that return multiple instances of **X** at a time
              - Also generated for various class combinations
            - **DbXs.scala** - The root access point for multiple instances of **X**
              - Also generated for various class combinations
        - description
          - **DbXDescriptions.scala** - The root access point for accessing multiple **X**-descriptions at once
            - Only generated for classes which support descriptions

## Generating Model Templates
There's an alternative mode available. This mode reads one or more tables from a database and writes 
a model structure template based on the information available.  
The purpose of this tool is to make it easier to migrate to Vault-Coder and Utopia -style code when starting with 
an existing database.

### Command Arguments
You may activate this alternative mode by writing `read` as the first command line argument.  
You may also specify the following additional arguments:
1. `user` (`u`) - Username used when connecting to the database (default = `root`)
2. `password` (`pw`) - Password used when connecting to the database (asked if not provided here)
3. `connection` (`con`) - Database address to connect to (default = `jdbc:mysql://localhost:3306/`)
4. `database` (`db`) - Name of the database to read (asked if not provided here)
5. `table` (`t`) - Name of the table or tables to read (use json array syntax for multiple tables, 
  alternatively you may write `-A` to read all tables, asked if not specified here)
6. `output` (`out`) - Path to the directory where the generated file will be written (default = `output`)