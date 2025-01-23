# Utopia Echo - List of Changes

## v1.2 (in development)
This update adds saving & loading to the **Chat** interface, improving it in other ways as well. 
Besides the **Chat** changes, this update irons out some bugs & missing interfaces for (streamed) Ollama response parsing.
### Breaking changes
- Changed **Chat**'s auto-summarization parameter syntax
### Bugfixes
- Chat messages now automatically convert tabs to spaces
- `newTextPointer` in **OllamaResponse** implementations now correctly fires change events 
  even when receiving a similar text update twice in a row (e.g. when encountering a paragraph change).
### New features
- Added saving & loading to **Chat**
- **Chat**'s summarization process now supports message-exclusion
- Added **ToolFactory** for loading tools from models
- Added model conversions to **ModelSettings**
- Added **BufferedOllamaResponse** and **BufferedOllamaResponseLike** traits, which support json extraction
### New methods
- **Chat**
  - Added `.copy`
- **Tool**
  - Added a new constructor
### Other changes
- Removed some print-lines that were left from previous tests
- Modified timeout settings in **OllamaClient**
- Modified **Chat** default parameters
- **Chat** now extends **ScopeUsable**
- **StreamedOllamaResponseParser** now utilizes volatile pointers

## v1.1 - 04.10.2024
A major update adding a number of new features, including a complete chat interface and tool support. 
Also includes some refactoring in order to accommodate this new features and their requirements.
### Breaking changes
- Refactored how requests are constructed
  - The current implementation is largely based on **GenerateParams** and **ChatParams**
- Renamed **Chat** to **ChatRequest**
  - Also renamed the subclasses accordingly
- Streaming-based requests, as well as instances of **StreamedOllamaResponseParser** 
  now require an implicit **Logger** construction parameter
  - This was added in order to manage errors within the managed pointers 
    (possibly introduced via external change event listeners)
- Some changes to **LlmDesignator**:
  - Moved **LlmDesignator** to `llm` package
  - **LlmDesignator** is now a trait instead of a case class
  - Renamed `name` to `llmName`
- Renamed **OllamaResponse** to **OllamaResponseLike** and added a new **OllamaResponse** trait 
  which doesn't have a generic type parameter
- Modified **StreamedOllamaResponseParser** trait (only affects custom response-parsers)
### Bugfixes
- Added the missing `model` parameter to the chat requests
### New features
- Added new **Chat** interface which manages conversation context (context size, conversation history & tools)
- Requests now support custom options (e.g. changing the temperature or context size)
- Chat requests now support tools
- Added a request for listing locally available models (**ListModelRequest**)
- Added a request for showing model information (**ShowModelRequest**)
- Added requests for pulling model data (**PullStreamingRequest** & **PullWithoutStatusRequest**)
- Streamed requests (**StreamedReply** and **StreamedReplyMessage**) now contain property `newTextPointer`
- Added **StreamedResponseParser** which makes creating Ollama-compatible response-parsers easier 
  (in case you need to add your own)
### New methods
- **OllamaClient**
  - Added `.localModels` which fires a **ListModelsRequest**
- **Prompt**
  - Added various `.toQuery` functions
- **ReplyMessage** (object)
  - Added `.from(OllamaResponse)` and `.async(Future)`
- **Query**
  - Added `.toRequestParams`
- **StreamedReply**
  - Added `.printAsReceived(...)`
### Other changes
- Built with Scala v2.13.14
- Multi-line queries no longer use `"""` as wrappers
- Added some toString implementations

## v1.0 - 28.07.2024
Initial version. See README for more details.
