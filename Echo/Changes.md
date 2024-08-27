# Utopia Echo - List of Changes

## v1.1 (in development)
### Breaking changes
- Streaming-based requests, as well as instances of **StreamedOllamaResponseParser** 
  now require an implicit **Logger** construction parameter
  - This was added in order to manage errors within the managed pointers 
    (possibly introduced via external change event listeners)
### Other changes
- Built with Scala v2.13.14

## v1.0 - 28.07.2024
Initial version. See README for more details.
