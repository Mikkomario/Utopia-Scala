# Utopia Logos
A system for storing and indexing textual information

Notice: This library is still in relatively early development. 
Consider this a beta product likely to change once additional development effort is applied.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Vault](https://github.com/Mikkomario/Utopia-Scala/tree/master/Vault)

## Required External Libraries
This module uses [emoji-java library](https://github.com/vdurmont/emoji-java) to wrap text emojis before they 
are stored in the database. Emoji-java is available under the MIT license.

You can find the jar used in development in the `lib` folder.

## Main Features
A database structure and interface for storing textual information in an indexed format
- Useful for storing large amounts of textual information 
  in a format that enables very fast searches and further analysis

## Implementation Hints
Before you can use this module, you need to set up the database structure. 
See the documents within the `data` directory, especially the `sql` directory, for these resources. 

Also, at the beginning of program execution, you need to call `LogosContext.setup(...)`.

Use the objects available under `utopia.logos.database.access` in order to interact with the database models.  
This project follows basic **Vault-Coder** architecture. 
For more information about the models and their relationships, consult the documentation in `data/doc` directory.