# Utopia Manuscript
*When you want to use Models (Flow) when parsing Excel files*

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Paradigm](https://github.com/Mikkomario/Utopia-Scala/tree/master/Paradigm)

## Required External Libraries
This library uses [Apache POI](https://poi.apache.org/) adn Apache POI ooxml libraries.  
These are available at Maven under: `org.apache.poi:poi` and `org.apache.poi:poi-ooxml`. 
In development, version 5.2.5 was used.

Apache POI is available under the [Apache 2.0 license](https://poi.apache.org/legal.html).

## Main features
This module is aimed to make Excel-parsing easier, while utilizing **Flow**'s soft typing interface.  
In the future, this module may also expand to cover other types of files, such as PDFs.

When it comes to Excel-parsing, here are the main features:
- Access specific spreadsheets without fully knowing their index, or even their name
- Automatically identify the header row
- Automatically convert rows into **Models** that support soft typing
- Support for alternative header names (which are automatically resolved)

## Implementation hints
Currently supported file types are **.xls** and **.xlsx**

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

This module is very small. You can get further by simply consulting the source code and its scaladocs.