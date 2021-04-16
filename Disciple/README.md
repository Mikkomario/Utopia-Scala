# Utopia Disciple

## Parent Modules
- Utopia Flow
- Utopia Access

## Main Features
Simple Request and Response models
- Immutable Requests make request creation very streamlined
- Support for both streamed and buffered responses

Singular interface class for all request sending and response receiving
- **Gateway** class wraps the most useful *Apache HttpClient* features and offers them via a couple of simple 
  methods
- Supports both callback -style and Future style response handling
- Supports parameter encoding
- Supports various response styles, including JSON and XML, as well as custom response styles or raw data
- Supports file uploading

## Implementation Hints

### Required external libraries
**Utopia Disciple** requires following jars from **Apache HttpClient**. The listed versions (v4.5) are what I've used in
development. You can likely replace them with later versions just as well.
- httpclient-4.5.5.jar
- httpcore-4.4.9.jar
- commons-codec-1.10.jar
- commons-logging-1.2.jar

### You should get familiar with these classes
- **Gateway** - This is your main interface for performing http requests and for specifying global request settings
- **Request** - You need to form a **Request** instance for every http interaction you do
- **BufferedResponse** - When you need to deal with server responses (status, response body, etc.)