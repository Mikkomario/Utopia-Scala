# Utopia BunnyMunch

## Parent Modules
- Utopia Flow

## Main Features
Efficient json parsing with support for typeless values
- **JsonBunny** object offers a very simple interface for converting json into values, which can then be 
  read in various other types.
  
## Implementation Hints

### You should get familiar with these classes
- **JsonBunny** - This object lets you parse values from json strings, files or streams
- **ValueFacade** - In case you wish to use the *Jawn* Parser interface, you can use ValueFacade to add
  support for typeless values.

# Jawn Copyright and License
This module uses Jawn library in JSON parsing and only introduces wrappers around it.

The following is from Jawn Github page: https://github.com/typelevel/jawn

    All code is available to you under the MIT license, available at http://opensource.org/licenses/mit-license.php.
    Copyright Erik Osheim, 2012-2020.