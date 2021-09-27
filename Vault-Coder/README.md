# Utopia Vault Coder
This application allows you to automatically write the basic SQL and Scala code to support your model structure. 
Instead of writing the code manually, you only need to specify a .json file which describes the content to create.

## Usage Instructions
First, you need to prepare a .json file describing the model structure you want to create / use. 
See the required document structure below.  

Once you have created a specification document, run the application using the following command line command: 
`java -jar Vault-Coder.jar <common path> <input file path> <output directory path>`
- The `<common path>` argument specifies the path (beginning) that is common for both the input and the output
- The `<input file path>` argument should point towards your specification .json file or to a directory that 
  holds such files. This should be a relative path after the `<common path>`. If empty, `<common path>` is used. 
  If both are empty, `./input` is used.
- The `<output directory path>` argument specifies a path to where the generated code will be stored. 
  - Defaults to `<common path>/output` or `./output` depending on whether the <common path> has been specified.
  - If the target directory doesn't exist, it will be created

The program will inform you if there were any problems during input file processing or during output write operations.

## Input File Structure
The input .json file should contain a single object with following properties:
- **"base_package" / "package": String (optional)** - Contains the package prefix common to all generated files 
  (e.g. "utopia.vault.coder")
- **"enumerations" / "enums": Object (optional)** - Contains a property for each introduced enumeration. 
  Each property should have a string array value where each item represents a possible enumeration value.
- **"classes" / "class": Object** - Contains a property for each introduced sub-package, the value of each 
  sub-package property should be a class object or an array of **class objects**.

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
- **"properties" / "props": [Object]** - An array of **property objects** for this class

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
  - **"creation" / "created"** - Results in Instant / Timestamp type and is used as the row creation time 
    (default name: "created")
    - Enables creation time -based indexing
  - **"deprecation" / "deprecated"** - Results in Instant / Timestamp type that is null / None while the item is 
    active (default name: "deprecatedAfter")
    - Enables item deprecation
  - **"expiration" / "expires"** - Results in Instant / Timestamp type that represents item expiration time 
    (default name: "expires")
    - Enables (timed) item deprecation
  - **"option[X]"** - Results in Option / nullable type where the underlying type is determined by what X is
  - **"enum[X]"** - Results in an enumeration value choice. Replace X with the enumeration of your choice 
    (must be specified in the "enumerations" -property). Uses the enumeration's name as the default property name.
    - Can be wrapped in option
  - If omitted or empty, defaults to Int for references and String if the length property is present
- **"length": Int (optional)** - Determines maximum text length if type "text" is used
- **"doc": String (optional)** - Description of this property (used in documentation)
- **"usage": String (optional)** - Additional description about the use of this property

## Output Format
This application will produce the following documents 
(where **X** is replaced by class name and **P** by class-related package name):
- **database_structure.sql** -document that contains create table sql statements
- model
  - enumeration
    - **E.scala** where **E** goes through each introduced enumeration
  - partial
    - **P**
      - **XData.scala** - The data model containing basic model information
  - stored
    - **P**
      - **X.scala** - A stored variant of the class model
- database
  - **Tables.scala** - An object containing a reference to all tables listed in the **database_structure.sql** document
  - factory
    - **P**
      - **XFactory.scala** - A factory object used for reading models from database
  - model
    - **P**
      - **XModel.scala** - Database interaction model class + the associated companion object used for forming queries etc.
  - access
    - single
      - **P**
        - **UniqueXAccess.scala** - A trait common to distinct single access points that return instances of **X**
        - **DbX.scala** - The root access point for individual instances of **X**
    - many
      - **P**
        - **ManyXsAccess.scala** - A trait common to access points that return multiple instances of **X** at a time
        - **DbXs.scala** - The root access point for multiple instances of **X**