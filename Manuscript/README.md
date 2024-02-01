# Utopia Manuscript
*When you want to use Models (Flow) when parsing Excel files*

## Main features
This module is aimed to make Excel-parsing easier, while utilizing **Flow**'s soft typing interface.  
In the future, this module may also expand to cover other types of files, such as PDFs.

When it comes to Excel-parsing, here are the main features:
- Access specific spreadsheets without fully knowing their index, or even their name
- Automatically identify the header row
- Automatically convert rows into Models that support soft typing

## Implementation hints
Currently supported file types are **.xls** and **.xlsx**

This module requires the following libraries in order to function correctly:
- **Utopia Flow**
- **Utopia Paradigm**
- **Apache POI** (At Maven: org.apache.poi:poi this version was built using the 5.2.5 version)
- **Apache POI ooxml** (At Maven: org.apache.poi:poi-ooxml)

Here's a template code to get you started:
```
import utopia.flow.parse.file.FileExtensions._

val result = Excel.open("test.xlsx") { excel =>
    excel(FirstSheet).map { sheet => 
        val modelsIter = sheet.modelsIteratorCompletingHeaders(Vector("header1", "header2"))
        // Do something with the parsed models
    }
}.flatten
```