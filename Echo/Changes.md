# Utopia Echo - List of Changes

## v1.6 (in development)
### Breaking changes
- Token counts are now represented using **TokenCount** instead of **Int**
- Token-counting is now based on an implicit **TokenCounter** parameter instead of **EstimateTokenCount**
- Partially rewrote **VastAiChatExecutor**, so that it uses a large max context size on all instances, but manages 
  the amount of processed tokens more carefully.
  - The constructor is now different, because of this
  - The return value of `.status` is also different
- **LlmServiceClient**'s `maxParallelRequests` parameter is now **Option** instead of **Int**, 
  and requests are no longer limited by default.
- **VastAiVllmProcess** now accepts `maxParallelRequests: Option[Int]`
### New features
- Token counting implementation may now be customized by extending and defining an implicit **TokenCounter**
### New methods
- **StatelessBufferedReplyGeneratorLike**
  - Added `.mapExpectedReplySize(...)` and `.mapExpectedThinkSize(...)`
- **ByteCount**
  - Added support for various operators
- **VastAiVllmProcess**
  - Added `.maxContextSize`
  - Added `.client` and `.clientPointer`
- **VastAiVllmProcessState**
  - Added `.isUnusable`
### Other changes
- Modified **TokenUsage**'s `.toString` implementation

## v1.5 - 15.03.2026
This major update introduces the following completely new features:
- **Vast AI** integration - Enabling renting of GPUs for LLM-hosting
- **Open AI**, **vLLM** & **DeepSeek** integration for simple buffered chat requests
- Support for different new thinking configurations in **Ollama**, **vLLM**, etc.
### Breaking changes
- Thinking is now controlled using a reasoning effort property, instead of a thinking enabled / thinks property 
- **ChatLike** now tracks `largestReplySize` and `largestThinkSize` separately
### Deprecations
- Deprecated **EchoContext**; Parsing failure **Status** is now defined in `Response.parseFailureStatus` instead.
- **ChatRequest** and **BufferedChatRequest** are now named **OllamaChatRequest** and **BufferedOllamaChatRequest**
- Deprecated `.thinkingContextSize` in **ChatLike**
- In **ChatLike**, `.additionalThinkingContextSize` is now named `.expectedThinkSize`
### Bugfixes
- Fixed a bug where **AbstractChat**'s `queueSize` would never reset after failing to start a chat request 
  (on context overflow) 
### New features
- Added **Vast AI** integration, enabling the hosting of LLM services on rented GPUs
  - See the following new classes:
    - **VastAiVllmChatExecutor**: A functional buffering chat interface utilizing multiple rented GPUs at once - 
      effective for large-scale parallel processing.
    - **VastAiVllmProcess**: An interface for setting up an individual **vLLM** server on a rented GPU
    - **VastAiProcess**: An interface for renting an individual GPU
- Adding preliminary (beta) interfaces for sending buffered chat requests using Open AI, DeepSeek and vLLM
  - Only text-based non-streaming requests are supported at this time
- Added **BufferedReplyGenerator** and **StatelessBufferedReplyGenerator** traits for streamlining buffered reply 
  -acquisition.
- Added **ContextSizeLimits**, **HasContextSizeLimits**, etc. for standardizing context size calculation
### Other changes
- Added support for Ollama's latest thinking response structuring
- **EstimateTokenCount** is now also trained in situations where thinking is enabled
- **ChatLike** now extends **HasMutableContextSizeLimits**
- Modified **ChatLike**'s `.toModel` implementation

## v1.4 - 01.11.2025
This update introduces two new integrations:
1. ComfyUI for image-generation
2. Piper for text-to-speech

Both require a separate locally running service in order to function.

Besides these, this update focuses on refactoring and generalization, 
that are necessary for eventually adding Open AI support.  
While there are a lot of breaking changes, 
I think you'll find the new structure and naming logic easier to use in the long run.

### Breaking changes
- Reworked the response / reply class hierarchy:
  - There are no longer separate **Reply** and **Response** classes. All classes now extend **Reply** / **ReplyLike**.
  - There are no longer separate **Streaming** reply / response classes, the **Reply** traits replaced those
  - **BufferedReply** classes remain as separate subclasses of **Reply**
  - Removed **BufferedOrStreamed** response / reply variants altogether; These are replaced with the **Reply** classes.
  - All Ollama-specific reply classes are placed under **OllamaReply**
- Reworked Ollama chat & generate request hierarchy:
  - Removed the **Streamed** and **BufferedOrStreamed** request variants 
    and joined the **BufferedOrStreamed** functionality under the main request trait / companion object
    (**ChatRequest** or **GenerateRequest**)
  - Renamed **Generate** to **GenerateRequest**
- Simplified streamed (Ollama) response parsing class hierarchy:
  - Renamed **StreamedResponseParser** to **StreamedNdJsonResponseParser**, 
    in order to more clearly communicate the NDJSON format expectation / dependency
  - **StreamedOllamaResponseParser** no longer accepts a generic type parameter, 
    since it'll only yield **OllamaReply**.
  - Deleted the separate classes for generate and chat -endpoint response parsing and added new versions of 
    those under **StreamedOllamaResponseParser**'s companion object (see the new `.chat` and `.generate` properties)
- Added a new `thoughts` property to **ReplyLike** and **ChatMessage**.
- Divided the **Chat** interface into 4 separate classes:
  1. **ChatLike** - the generic trait that accepts type parameters without specifying full implementation
  2. **Chat** - A variation of **ChatLike**, which removes the generic type parameters, replacing them with the generic 
    **Reply** and **BufferedReply** types
  3. **AbstractChat** - Which specifies most of the missing **ChatLike** implementation, 
    leaving room for Ollama / OpenAI -specific functionality
  4. **OllamaChat** - Full implementation matching the previous **Chat** class
     - Note: Modified **OllamaChat**'s constructor, so that it now accepts an implicit **OllamaClient** reference, 
       instead of an explicit **RequestQueue** reference.
- Did some package refactoring:
  - **ChatParams** and the `tool` package are now located directly under `utopia.echo.model.request`
  - **Chat** classes are now located under `utopia.echo.controller.chat`
- **ToolCall** now requires a new property: `callId: String`, which may be empty
  - This property will be used in the OpenAI integration
- Modified **OllamaClient** constructor to accept a **Gateway** instance
  - The previous constructor is available as an `.apply(...)` method in the companion object
### New features
- Added a basic [ComfyUI](https://www.comfy.org/) integration for generating images using stable diffusion
  - The current implementation is extendable to building custom workflows, 
    but concrete / full implementation is limited to simple image generation.
- Added [Piper](https://github.com/OHF-Voice/piper1-gpl) integration, which may be used for simple text-to-speech
  - This includes **PiperClient**, as a **RequestQueue** interface, plus **TextToAudioFileRequest**, 
    and the more generic **TextToSpeechRequest**.
- Added more advanced support for thinking LLMs:
  - Context size is maximized in order to ensure that the thinking process fits
  - Thinking may be deactivated
  - Think content won't be included in the chat history sent to the LLM
  - In **buffered** replies, the <think> block contents are now separated to `thoughts` and not included in `text`
- Added models for interacting with Open AI, but there doesn't yet exist a full **Chat** interface for Open AI, 
  nor have these been tested in any way.
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
