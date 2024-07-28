# Utopia BunnyMunch

## Parent Modules
- Utopia Flow

## Main Features
Efficient json parsing with support for typeless values (i.e. for **Flow**'s **Value** interface)
- **JsonBunny** object offers a very simple interface for converting json into **Value**s, which can then be 
  read in various other types.
- **AsyncJsonBunny** object provides functions for asynchronous json-processing, 
  which may be useful when reading large json files or streams.
  
## Implementation Hints

### You should get familiar with these classes
- **JsonBunny** - This object lets you parse values from json Strings, files and **InputStream**s
- **ValueFacade** - In case you wish to use the [Jawn](https://github.com/typelevel/jawn) Parser interface, 
  you can use **ValueFacade** to add support for typeless values.

# Jawn Copyright and License
This module uses [Jawn](https://github.com/typelevel/jawn) library in JSON parsing and only adds wrappers for it.

The following is from Jawn Github page: https://github.com/typelevel/jawn (referenced 12.7.2024)

    All code is available to you under the MIT license, available at http://opensource.org/licenses/mit-license.php.
    Copyright Erik Osheim, 2012-2022.