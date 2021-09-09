# Utopia Vault Coder
This application allows you to automatically write the basic SQL and Scala code to support your model structure. 
Instead of writing the code manually, you only need to specify a .json file which describes the content to create.

## Usage Instructions
First, you need to prepare a .json file describing the model structure you want to create / use. 
See the required document structure below.  

Once you have created a specification document, run the application using the following command line command: 
`java -jar Vault-Coder.jar <input file path> <output directory path>`
- The `<input file path>` argument should point towards your specification .json file or to a directory that 
  holds such files. This parameter defaults to a directory called "input" within the directory from which you run 
  this application.
- The `<output directory path>` argument defaults to a directory named "output" within the directory from which you run 
  this application. If the output directory doesn't exist, one will be created.

The program will inform you if there were any problems during input file processing or during output write operations.

## Input File Structure
The input .json file should contain a single object with following properties:
- **"base_package": String** - Contains the package prefix common to all generated files (e.g. "utopia.vault.coder")
- **"classes": Object** - Contains a property for each introduced sub-package, the value of each sub-package property 
  should be a class object or an array of **class objects**.

### Class Object Structure
Class objects should contain following properties:
- **"name": String** - Name of this class as it appears in the Scala code (e.g. "TestClass")
- **"name_plural": String (optional)** - Plural version of the class name (e.g. "TestClasses"). 
  By default, "s" is added to the end of the name property in order to form the plural name.
- **"use_long_id": Boolean (optional)** - Whether Long should be used in the id property instead of Int. 
  Defaults to false.
- **"doc": String (optional)** - A description of this class, which is written to class documentation
- **"properties": [Object]** - An array of **property objects** for this class

#### Property Object Structure
Each property object should contain following properties:
- **"name": String** - Name of this property as it appears in the Scala code (e.g. "testProperty")
- **"name_plural": String (optional)** - The plural form of this property's name (e.g. "testProperties"). 
  By default, "s" is added to the end of the standard property name in order to form the plural form.
- **"references": String (optional)** - Name of the database table referenced by this property. 
  Omit or leave empty if this property doesn't reference any other class.
- **"type": String (optional)** - The data type of this property. The following options are accepted:
  - **"text"** - Results in a String / Varchar type
  - **"int"** - Results in Int numeric type
  - **"long"** - Results in Long numeric type
  - **"double"** - Results in Double numeric type
  - **"boolean"** - Results in Boolean type (true / false)
  - **"datetime"** - Results in Instant / Datetime type
  - **"date"** - Results in LocalDate / Date type
  - **"time"** - Results in LocalTime / Time type
  - **"creation"** - Results in Instant / Timestamp type and is used as the row creation time
  - **"option[X]"** - Results in Option / nullable type where the underlying type is determined by what X is
  - If omitted or empty, defaults to Int for references and String if the length property is present
- **"length": Int (optional)** - Determines maximum text length if type "text" is used
- **"doc": String (optional)** - Description of this property (used in documentation)
- **"usage": String (optional)** - Additional description about the use of this property

## Output Format
This application will produce the following documents 
(where **X** is replaced by class name and **P** by class-related package name):
- **database_structure.sql** -document that contains create table sql statements
- model
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