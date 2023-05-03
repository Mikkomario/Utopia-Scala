# Utopia Firmament - List of Changes

## v1.1 (in development)
### Breaking Changes
- The icon parameter **WindowButtonBlueprint** is no longer an **Option**. An empty **SingleColorIcon** acts as "no icon"
- **SingleColorIconCache** now requires an implicit **Logger** parameter
### New Methods
- **LocalizedString** (object)
  - Added `.alwaysEmpty`
- **SingleColorIcon** (object)
  - Added `.alwaysEmpty`
### Other Changes
- **SingleColorIconCache** now logs errors and appends ".png" to file names in case the file extension is not specified
- **SingleColorIcon** now extends **MayBeEmpty**

## v1.0 - 01.05.2023
Initial version  
The following features were moved over from **Reflection**
- Generic component traits
- Generic container traits
- Stacking
- Component creation context classes (which were rewritten)
- Localization classes
- Icon support
- Custom drawing tools and traits
- Generic container implementation templates
- Container content management tools and traits
- Awt utilities

Basically everything that was not exclusive to **Reflection** was moved over to upper modules.  
This is in order to separate **Reach** from **Reflection**.
