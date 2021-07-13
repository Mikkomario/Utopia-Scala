# Utopia Citadel Description Importer
This project serves as a utility tool for quickly importing / updating new descriptions on pre-existing 
database items (like tasks or description roles etc.). The definition of these values in 
raw SQL is difficult, therefore this project allows you to write these documents in a format 
where you don't need to specify link or description indices.

## Usage Instructions
You need to specify following parameters as either command line arguments or in a separate settings .json file 
which you can specify. By default, the program searches for the settings in the directory from which you run 
the program, looking for 'description-importer-settings.json' file. You can override this by specifying 
settings=<file path> or read=<file path> as a command line argument.  

Here are the parameters you need to specify. The alias for the parameter is in parentheses. 
You may specify the parameter by writing <parameter name or alias>=<parameter value>. 
Alternatively, you can omit the parameter name altogether as long as you keep the parameters in the order listed below.
- **input (in): File Path** - A relative or absolute path to the file you want to import. 
  You may specify a directory if you want to import all .json files in that directory.
- **database (db): String** - Name of the database to access (default = exodus_db)
- **user (u): String** - Username to use when accessing the database (default = root)
- **password (pw): String (optional)** - Password to use when accessing the database. 
  You can leave this unspecified or empty if your database doesn't use a password.
- **settings (read): File Path** - A relative or absolute path to a .json settings file that specifies 
  some of these values
  
You may read the same descriptions multiple times without ill effects. The program will only update the descriptions 
that were added or modified.

## Input File Format
The program reads the descriptions from .json files. The files should contain a json array with json objects inside. 
Each object should have the following structure:
- **target: Object or String** - For description types in the **Utopia Citadel** project, the name of the 
  description target table (E.g. "task") will suffice. For description types in other modules or specific to your 
  project, you have to specify an object with the following two properties:
  - **table: String** - Name of the **description link** table (E.g. "task_description")
  - **column: String** - Name of the **property** (not column) that refers to the target table (E.g. "taskId"). 
- **names**, **descriptions**, **purposes**, whatever matches a `.jsonKeyPlural` -property of 
  a **DescriptionRole** in your database (**Citadel** only provides "names" at this time). The value of this 
  property must be a json object with **language iso codes** as keys and objects as values.
  - Then, the format of these (3rd level) object values is as follows: Keys are item target ids (e.g, task ids) 
    and values are the descriptions as strings.  
    
Below is an example of possible description file contents:
`[
{
    "target": "user_role",
    "names": {
        "en": {
            "1": "owner",
            "2": "admin"
        }
    }
}, 
{
    "target": {
        "table": "fruit_description", 
        "column": "fruitId"
    }, 
    "names": {
        "en": {
            "1": "apple", 
            "2": "orange"
        }, 
        "fi": {
            "1": "omena", 
            "2": "appelsiini"
        }
    }, 
    "quotes": {
        "en": {
            "1": "an apple a day keeps the doctor away"
        }
    }
}
]`

You will find such files in the **data** folder of each **Utopia** module that uses **Utopia Citadel** and 
introduces new descriptions.