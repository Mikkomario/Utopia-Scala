# Utopia Echo - List of Changes

## v1.4 (in development)
Adding ComfyUI integration
### Breaking changes
- Renamed **ReplyMessage** classes to **OllamaReply** classes
- Modified reply constructors to include a new `thoughts` property. This also affects **ChatMessage**.
### Deprecations
- Deprecated all **Reply** classes. These are now implemented in **OllamaResponse** classes
### New features
- Added more advanced support for thinking LLMs:
  - Context size is maximized in order to ensure that the thinking process fits
  - `/nothink` may be inserted to the system message, if requested
  - Think content won't be included in the chat history sent to the LLM
  - In **buffered** replies, the <think> block contents are now separated to `thoughts` and not included in `text`
### Other changes
- When using `.jsonArray` and `.jsonObject` in **BufferedOllamaResponseLike**, 
  attempts to handle JSON generation errors where there's a comma before the array end.
  - Also, removes any line comments before parsing
  - Also, replaces `\\` with `\`
  - Also, the generated error messages now include the input
- **ResponseStatistics** and **GenerationDurations** now extend **Combinable** and **LinearScalable** 
  (i.e. they provide the `+` and `*` functions)

## v1.3 - 26.05.2025
This update improves **Chat** behavior. 
Notably, it adds support for token count estimation that learns from **Chat** usage.

There are also some preliminary models added to support Open AI, but these are not usable at this time.  
Package structure is also updated in order to add future support for Open AI, and possibly other platforms.
### Breaking changes
- Package restructuring:
  - Moved classes from `model.response` to `model.response.ollama`
- `EstimateTokenCount.in(String)` now yields an **EstimatedTokenCount** instead of **Int**
- Multiple potentially breaking changes to **Chat**
  - Modified **Chat** model conversions. Previously stored **Chat** instances might not behave exactly the same anymore.
  - Estimated default system message size is now 0 instead of 256 tokens
  - System message tokens and chat history tokens 
    are now measured in **PartiallyEstimatedTokenCount** instead of **Int**
  - Used context size is now measured in **PartiallyEstimatedTokenCount** instead of **UncertainInt**
  - The name of the first parameter is now `requestQueue` instead of `ollama`
### Bugfixes
- **BufferedReplyMessage** parsing was previously bugged
- **Tool** names were previously missing from the Ollama requests
- **Chat**`.conversationHistoryTokens` was previously bugged
### New features
- **EstimateTokenCount** now adjusts the estimations based on the feedback it receives. 
  **Chat** automatically provides feedback to **EstimateTokenCount**, when it receives statistics from the server.
- **Chat** now supports customized summarization prompting via 
  `.summarizationPromptPointer`, `.summarizationPrompt` and a new `.summarize(...)` parameter
### New methods
- **EstimateTokenCount**
  - Added `.feedback(Int, Int)` and `.train(String, Int)` for adjusting future estimations based on chat query results
  - Added `.continuallyIn(...)`, which reacts to feedback
- **OllamaClient**
  - Added `.bufferedResponseFor(...)`, a utility function for performing a **GenerateBuffered** request
### Other changes
- Multiple changes to **Chat** behavior:
  - Modified how chat history size is calculated, and how those calculations are updated as new information is received
  - `.push(...)` now fails if `maxContextSize` is reached, and no space remains for a response
  - Modified how `num_predict` is assigned when a custom `num_ctx` is specified via model parameters
  - Modified how a custom `num_predict` value is handled
  - **Chat** now accepts a **RequestQueue** instead of only **OllamaClient**

## v1.2 - 23.01.2025
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
