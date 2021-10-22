# Utopia Vault Coder
This application allows you to automatically write the basic SQL and Scala code to support your model structure. 
Instead of writing the code manually, you only need to specify a .json file which describes the content to create.

## Usage Instructions
First, you need to prepare a .json file describing the model structure you want to create / use. 
See the required document structure below.  

Once you have created a specification document, run the application using the following command line command: 
`java -jar Vault-Coder.jar <root> <input> <output> <filter> <group>`
- `<root>` argument specifies the path (beginning) that is common for both the input and the output
  - This argument is optional
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

The program will inform you if there were any problems during input file processing or during output write operations.

## Input File Structure
The input .json file should contain a single object with following properties:
- **"name" / "project": String (optional)** - Name of this project
  - If left empty, the name will be based on the specified base package or database package
  - If your project contains multiple modules, name the Vault-dependent module
- **"author": String (optional)** - Author of this project's model structure
- **"base_package" / "package": String (optional)** - Contains the package prefix common to all generated files 
  (e.g. "utopia.vault.coder")
- **"model_package": String (optional)** - Package where all the model classes are written
  - Defaults to base_package.model
- **"database_package" / "db_package": String (optional)** - Package where all the database -related classes are written
  - Defaults to base_package.database
- **"models_without_vault": Boolean (optional)** - Whether model classes can't contain database references 
  (Metropolis-style project) (default = false)
- **"enumerations" / "enums": Object (optional)** - Contains a property for each introduced enumeration. 
  Each property should have a string array value where each item represents a possible enumeration value.
- **"referenced_enumerations" / "referenced_enums": [String]** - Paths to enumerations from other projects
  - Enumeration paths should include the full reference: E.g. "utopia.vault.coder.model.enumeration.PropertyType"
- **"classes" / "class": Object** - Contains a property for each introduced sub-package, the value of each 
  sub-package property should be a class object or an array of **class objects** (see structure below).

### Class Object Structure
Class objects should contain following properties:
- **"name": String (optional)** - Name of this class as it appears in the Scala code (e.g. "TestClass")
  - If not specified, the name is parsed from the specified table name
- **"name_plural": String (optional)** - Plural version of the class name (e.g. "TestClasses"). 
  By default, "s" is added to the end of the "name" property in order to form the plural name.
- **"table_name" / "table": String (optional)** - Name of the SQL table generated for this class (e.g. "test_class")
  - If not specified, the class name is converted to 'under_score' format and used as the table name
  - Either this or "name" must be provided
- **"use_long_id": Boolean (optional)** - Whether Long should be used in the id property instead of Int. 
  Defaults to false.
- **"doc": String (optional)** - A description of this class, which is written to class documentation
- **"author": String (optional)** - Author of this class. If omitted, the project author will be used.
- **"combinations" / "combos": [Object]** - An array of **combination objects** for this class (see structure below)
  - Alternatively, you can specify a **"combination"** or **"combo"** -property with only a single 
    **combination object** as a value. Please don't specify both, however. 
- **"description_link_column" / "description_link" / "desc_link": String (optional)** - Specifies the name of 
  the column in a description link table that refers to this class (e.g. "test_class_id")
  - Please note that if you specify this value, localized description support is added for this class
    - This requires the implementing project to use **Utopia Citadel**
- **described: Boolean (optional)** - Determines whether this class should have localized description support, 
  which is a **Utopia Citadel** -specific feature
  - Defaults to `false`
  - This value is ignored if "description_link_column" is specified
  - If set to `true`, "description_link_column" value will be autogenerated based on the table name of this class
- **"properties" / "props": [Object]** - An array of **property objects** for this class (see structure below)
- **"combo_indices" / "indices": [[String]]** - An array of multiple key indices, each written as an array of property names
  - Alternatively you can provide **"combo_index"** or **"index"** property that contains a single multiple 
    key index array

#### Combination Object Structure
Each combination object should contains following properties:
- **"child" / "children": String** - Name of the linked child class
  - NB: If you use the name "children" and don't otherwise specify combination type, 1-n -linking will be used. 
    For "child" property name, combination type 1- 0-1 will be used instead.
- **"type": String (optional)** - Type of this combination, from one of the options below:
  - **"one" / "link"** => Uses 1 to 1 linking (child must always be present)
  - **"option" / "optional"** => Uses 1 to 0-1 -linking where there can be cases without a child
  - **"multi" / "many"** => Uses 1-n -linking
    - If you use this combination type, you may specify `"is_always_linked": Boolean` -property to specify how to treat 
      the 0 children case (default = not always linked => 0 children case is valid)
  - If you omit this property, the combination type is interpreted based on child property name, 
    the referred child format and the "isAlwaysLinked" property.
- **"is_always_linked" / "always_linked": Boolean (optional)** - Applies to 1-n links, determining whether to always 
  expect there to be children in queries
- **"name": String (optional)** - Name of the combined class
  - If omitted, "ParentWithChild" (or "ParentWithChildren") -type of naming will be used
- **"parent_alias" / "alias_parent": String (optional)** - Name to use for the parent class within this 
  combination class
  - Also affects combination naming if "name" is not specified
- **"child_alias" / "alias_child": String (optional)** - Name to use for the child / children in the combined class
  - Also affects combination naming if "name" is not specified

#### Property Object Structure
Each property object should contain following properties:
- **"name": String (optional)** - Name of this property as it appears in the Scala code (e.g. "testProperty")
  - If not specified, a camelCased column name is used. Alternatively the name is based on the data type used. 
    See data type list below for the default names.
- **"name_plural": String (optional)** - The plural form of this property's name (e.g. "testProperties"). 
  By default, "s" is added to the end of the standard property name in order to form the plural form.
- **"references" / "ref": String (optional)** - Name of the database table referenced by this property. 
  Omit or leave empty if this property doesn't reference any other class.
- **"type": String (optional)** - The data type of this property. The following options are accepted:
  - **"text" / "string" / "varchar"** - Results in a String / Varchar type (default name: "name" / "text")
  - **"int"** - Results in Int numeric type (default name: "index")
  - **"long" / "bigint" / "number"** - Results in Long numeric type (default name: "number")
  - **"double"** - Results in Double numeric type (default name: "amount")
  - **"boolean" / "flag"** - Results in Boolean type (true / false) (default name: "flag")
  - **"datetime" / "timestamp"** - Results in Instant / Datetime type (default name: "timestamp")
  - **"date" / "LocalDate"** - Results in LocalDate / Date type (default name: "date")
  - **"time" / "LocalTime"** - Results in LocalTime / Time type (default name: "time")
  - **"days"** - Results in Days type (default name: "duration")
  - **"duration[X]"** - Results in a time duration type (default name: "duration")
    - **X** determines the unit used when storing the duration to the database. Available options are:
      - **h / hour / hours** - Stores as hours (int)
      - **m / min / minute / minutes** - Stores as minutes (int)
      - **s / second / seconds** - Stores as seconds (int)
      - **ms** - Stores as milliseconds (bigint, default)
  - **"creation" / "created"** - Results in Instant / Timestamp type and is used as the row creation time 
    (default name: "created")
    - Enables creation time -based indexing (if indexed)
    - Indexes by default
  - **"deprecation" / "deprecated"** - Results in Instant / Timestamp type that is null / None while the item is 
    active (default name: "deprecatedAfter")
    - Enables item deprecation
    - Indexes by default
  - **"expiration" / "expires"** - Results in Instant / Timestamp type that represents item expiration time 
    (default name: "expires")
    - Enables (timed) item deprecation
    - Indexes by default
  - **"option[X]"** - Results in Option / nullable type where the underlying type is determined by what X is
  - **"enum[X]"** - Results in an enumeration value choice. Replace X with the enumeration of your choice 
    (must be specified in the "enumerations" -property). Uses the enumeration's name as the default property name.
    - Can be wrapped in option
  - If omitted or empty, defaults to Int for references and String if the length property is present
- **"index" / "indexed": Boolean (optional)** - Set to true if you want to force indexing and false if you want 
  to disable it
  - Omit or leave as null if you want the indexing to be determined by the data type
  - References will always create an index, regardless of this value
- **"length": Int (optional)** - Determines maximum text length if type "text" is used
- **"doc": String (optional)** - Description of this property (used in documentation)
- **"usage": String (optional)** - Additional description about the use of this property

## Output Format
This application will produce the following documents 
(where **X** is replaced by class name, **P** by class-related package name 
and **S** is replaced with last project package name):
- **database_structure.sql** -document that contains create table sql statements
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
  - model
    - **SDescriptionLinkModel.scala** - An object that contains description link model factories for described classes
    - **P**
      - **XModel.scala** - Database interaction model class + the associated companion object used for forming queries etc.
  - access
    - single
      - **P**
        - **UniqueXAccess.scala** - A trait common to distinct single access points that return instances of **X**
        - **DbSingleX.scala** - A class that accesses individual instances of **X** based on their id
        - **DbX.scala** - The root access point for individual instances of **X**
      - description
        - **DbXDescription.scala** - The root access point for individual descriptions targeting instances of **X**
          - Only generated for classes which support descriptions
    - many
      - **P**
        - **ManyXsAccess.scala** - A trait common to access points that return multiple instances of **X** at a time
        - **DbXs.scala** - The root access point for multiple instances of **X**
      - description
        - **DbXDescriptions.scala** - The root access point for accessing multiple **X**-descriptions at once
          - Only generated for classes which support descriptions