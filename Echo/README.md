# Utopia Echo
An interface for interactions with **LLM**s (i.e. large language models), especially the 
[Ollama API](https://github.com/ollama/ollama/blob/main/docs/api.md).

[Ollama](https://ollama.com/) is an interface for hosting LLM's locally, 
which provides major benefits in terms of privacy, although requiring some computing power from the local device.

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia BunnyMunch](https://github.com/Mikkomario/Utopia-Scala/tree/master/BunnyMunch)
- [Utopia Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access)
- [Utopia Disciple](https://github.com/Mikkomario/Utopia-Scala/tree/master/Disciple)
- [Utopia Annex](https://github.com/Mikkomario/Utopia-Scala/tree/master/Annex)

## Main Features
An **Annex**-compatible interface for requesting a local LLM to generate an answer to a query
- Supports both streamed (word-by-word) and buffered response-parsing
  - Streamed parsing is more appropriate in text-based use-cases like chatbots, enabling more responsive user-interfaces
  - Buffered parsing is simpler and also more suitable for JSON-based use-cases, where partial responses 
    are unlikely to be useful.
- Supports advanced context setting, allowing you to more easily specify, what kind of content you wish to receive,
  especially in JSON-based use-cases

A complete chat interface with context size management, conversation history and tool support (Ollama feature).

Interfaces for listing and pulling model data.

## Implementation Hints

### Classes you should be aware of
- **OllamaClient** - The main interface for sending out requests to a (local) Ollama server
- **Chat** provides you with immediate access to continuous chats
- Various request models under `utopia.echo.model.request`
  - These are divided into 2 categories: **ChatRequest** and **Generate** requests, 
    which match different endpoints in the Ollama API.
  - Both of these requests have 3 variants each:
    - Buffered: Only one response is received
    - Streamed: Response text is received as a **Changing** instance, enabling reactive interfaces
    - BufferedOrStreamed: A wrapper which supports either of the above use-cases
  - You may get started with **StreamedChat** and **GenerateBuffered** -requests
- **Prompt** and **Query**, which are wrapper classes for the prompts / instructions sent to the LLMs
  - For advanced queries that expect JSON responses, familiarize yourself with **ObjectSchema**, also
- You will need at least one (implicit) **LlmDesignator** instance in order to use this interface. 
  **LlmDesignator**s are simply wrappers for targeted LLM names.
- When using **ChatRequests**, you will need to use the **ChatMessage** data-structure.