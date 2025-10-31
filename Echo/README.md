# Utopia Echo
An interface focused on interfacing with various (locally running) AI services:
  1. AI chat (LLMs) via [Ollama](https://ollama.com/)
  2. Image generation (stable diffusion) via [ComfyUI](https://www.comfy.org/)
  3. Text-to-Speech via [Piper](https://github.com/OHF-Voice/piper1-gpl?tab=readme-ov-file)

[Ollama](https://ollama.com/) is an interface for hosting LLM's locally.  
Local hosting provides major benefits compared to cloud services in terms of privacy. 
The lack of moderation and full transparency in terms of technology may also be considered a plus. 
However, these services often require some computing power from the local device (like a GPU with some VRAM).

## Parent Modules
- [Utopia Flow](https://github.com/Mikkomario/Utopia-Scala/tree/master/Flow)
- [Utopia BunnyMunch](https://github.com/Mikkomario/Utopia-Scala/tree/master/BunnyMunch)
- [Utopia Access](https://github.com/Mikkomario/Utopia-Scala/tree/master/Access)
- [Utopia Disciple](https://github.com/Mikkomario/Utopia-Scala/tree/master/Disciple)
- [Utopia Annex](https://github.com/Mikkomario/Utopia-Scala/tree/master/Annex)

## Main Features
An **Annex**-compatible interface for requesting a local LLM to generate answers to individual queries / prompts.
- Supports both streamed (word-by-word) and buffered response-parsing
  - Streamed parsing is more appropriate in text-based use-cases like chatbots, enabling more responsive user-interfaces
  - Buffered parsing is simpler and also more suitable for JSON-based use-cases, where partial responses 
    are unlikely to be useful.

An advanced chat interface with:
  1. Customizable conversation history, allowing for continuous conversations
  2. Context size management for optimizing memory consumption
  3. Tool support (i.e. providing actions for the LLM, like making DB queries (implemented separately))

Built-in support for image generation using **ComfyUI**, with support for building more complex workflows.

Support for simple text-to-speech requests using **Piper**.

## Implementation Hints

### Classes you should be aware of

#### LLM use-case
- **OllamaClient** - The main interface for sending out requests to a (local) Ollama server
- **Chat** - provides you with immediate access to continuous LLM interaction
  - If you don't want to use **Chat**, you can use the various request models under `utopia.echo.model.request.ollama`
    - These are divided into 2 categories: **ChatRequest** and **GenerateRequest**, 
      which match different endpoints in the Ollama API.
      - Most time you probably want to use **ChatRequest**.
    - Both of these requests can be sent in 2 forms:
      - Buffered: Only one response is received
      - Streamed: Response text is received as a **Changing** instance, enabling reactive interfaces
- **Prompt** and **Query**, which are wrapper classes for the prompts / instructions sent to the LLMs
  - For advanced queries that expect JSON responses, familiarize yourself with **ObjectSchema**, also
    - Warning: This feature hasn't been properly tested, and may be dropped in a future release
- You will need at least one (implicit) **LlmDesignator** instance in order to use this interface. 
  **LlmDesignator**s are simply wrappers for targeted LLM names.
- When using **ChatRequests**, you will need to use the **ChatMessage** data-structure.

#### Image-generation
- **GenerateImages** gives you a plug-to-play interface for starting image generation
  - You will need to have set up **ComfyUiDir** (a path reference), **ComfyUiClient** and a **CheckPointModel**
    - All of these are relatively trivial to set up. Call `CheckPointModel.list` if you don't have a direct model reference.
  - Once you've set up a test environment, try modifying **SamplerSettings** and the **Seed**
- For custom workflows, check out `model.comfyui.workflow.node` 
  and implement the node support you need by extending **WorkFlowNode**.
  - Check out **GenerateImages** source code for a simple example.

#### Text-to-speech
- You'll need to construct a **TextToSpeechRequest** and send it via **PiperClient**. Very simple.
  - Use **TtsParams** to customize your request, and **Voice** to specify which voice model to use.