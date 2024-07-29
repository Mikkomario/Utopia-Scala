# Utopia Citadel
**Citadel** provides database interactions related with user management (based on **Metropolis**).

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia Vault](https://github.com/Mikkomario/Utopia-Scala/tree/master/Vault)
- [Utopia Metropolis](https://github.com/Mikkomario/Utopia-Scala/tree/master/Metropolis)

## Main Features
This module provides database structure and database interaction classes for **Metropolis** features, 
for either server (**Exodus**) or for client-side use-cases (utilizing 
[Utopia Trove](https://github.com/Mikkomario/Utopia-Scala/tree/master/Trove)).

## Implementation Hints
You will have to import the database structure sql document contents to your database when you use this module. 
See [data](https://github.com/Mikkomario/Utopia-Scala/tree/master/Citadel/data) and 
[sql](https://github.com/Mikkomario/Utopia-Scala/tree/master/Citadel/data/sql) directories in this module.  
Also, in the **data** directory, you will find preset descriptions that you can import to your database using the 
[Citadel Description Importer](https://github.com/Mikkomario/Utopia-Scala/tree/master/Citadel-Description-Importer) 
application.

Before using **Citadel**, you must call `CidadelContext.setup(...)` method.

### Classes you should be aware of
- [Tables](https://github.com/Mikkomario/Utopia-Scala/blob/master/Citadel/src/utopia/citadel/database/Tables.scala) - 
  Please use this object when you need to access or refer to tables inside your database
- Please also check out the 
  [utopia.citadel.database.access package](https://github.com/Mikkomario/Utopia-Scala/tree/master/Citadel/src/utopia/citadel/database/access) 
  for various database-access interfaces.