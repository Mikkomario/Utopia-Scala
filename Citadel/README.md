# Utopia Citadel

## Parent Modules
- Utopia Flow
- Utopia Vault
- Utopia Metropolis

## Main Features
Database structure and classes for **Metropolis** features, 
for either server or client (**Trove**) side.

## Implementation Hints
You will have to import the database structure sql document contents to your database when you use this module.

Before using **Citadel**, you must call `CidadelContext.setup(...)` method.

### Classes you should be aware of
- **Tables** - Please use this object when you need to access or refer to tables inside your database