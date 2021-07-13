# Utopia Citadel

## Parent Modules
- Utopia Flow
- Utopia Vault
- Utopia Metropolis

## Main Features
This module provides database structure and database interaction classes for **Metropolis** features, 
for either server (**Exodus**) or client side (utilizing **Utopia Trove**).

## Implementation Hints
You will have to import the database structure sql document contents to your database when you use this module. 
See **sql** directory of this module.  
Also, in the **data** directory, you will find preset descriptions that you can import to your database using the 
**Citadel Description Importer** application.

Before using **Citadel**, you must call `CidadelContext.setup(...)` method.

### Classes you should be aware of
- **Tables** - Please use this object when you need to access or refer to tables inside your database
- Please also check out the `utopia.citadel.database.access` package for various simple access points.