# Logos
Version: **v0.3**  
Updated: 2024-08-27

## Table of Contents
- [Enumerations](#enumerations)
  - [Display Style](#display-style)
- [Packages & Classes](#packages-and-classes)
  - [Text](#text)
    - [Delimiter](#delimiter)
    - [Statement](#statement)
    - [Text Placement](#text-placement)
    - [Word](#word)
    - [Word Placement](#word-placement)
  - [Url](#url)
    - [Domain](#domain)
    - [Link](#link)
    - [Link Placement](#link-placement)
    - [Request Path](#request-path)

## Enumerations
Below are listed all enumerations introduced in Logos, in alphabetical order  

### Display Style
Represents a style chosen for displaying words, such as underlining or bold text. An open enumeration, meaning that sub-modules may introduce their own values.

Key: `id: Int`  
Default Value: **Default**

**Values:**
- **Default** (1) - The default style with no modifications on how the text should be displayed.

Utilized by the following 1 classes:
- [Word Placement](#word-placement)

## Packages and Classes
Below are listed all classes introduced in Logos, grouped by package and in alphabetical order.  
There are a total number of 2 packages and 9 classes

### Text
This package contains the following 5 classes: [Delimiter](#delimiter), [Statement](#statement), [Text Placement](#text-placement), [Word](#word), [Word Placement](#word-placement)

#### Delimiter
Represents a character sequence used to separate two statements or parts of a statement

##### Details
- Uses **index**: `text`

##### Properties
Delimiter contains the following 2 properties:
- **Text** - `text: String` - The characters that form this delimiter
- **Created** - `created: Instant` - Time when this delimiter was added to the database

##### Referenced from
- [Statement](#statement).`delimiterId`

#### Statement
Represents an individual statement made within some text. Consecutive statements form whole texts.

##### Details
- **Chronologically** indexed
- Uses **index**: `created`

##### Properties
Statement contains the following 2 properties:
- **Delimiter Id** - `delimiterId: Option[Int]` - Id of the delimiter that terminates this sentence. None if this sentence is not terminated with any character.
  - Refers to [Delimiter](#delimiter)
- **Created** - `created: Instant` - Time when this statement was first made

##### Referenced from
- [Link Placement](#link-placement).`statementId`
- [Word Placement](#word-placement).`statementId`

#### Text Placement
Places some type of text to some location within another text

##### Details

##### Properties
Text Placement contains the following 3 properties:
- **Parent Id** - `parentId: Int` - Id of the text where the placed text appears
- **Placed Id** - `placedId: Int` - Id of the text that is placed within the parent text
- **Order Index** - `orderIndex: Int`, `0` by default - 0-based index that indicates the specific location of the placed text

#### Word
Represents an individual word used in a text document. Case-sensitive.

##### Details
- Combines with [Word Placement](#word-placement), creating a **Stated Word**
- Uses **index**: `text`

##### Properties
Word contains the following 2 properties:
- **Text** - `text: String` - Text representation of this word
- **Created** - `created: Instant` - Time when this word was added to the database

##### Referenced from
- [Word Placement](#word-placement).`wordId`

#### Word Placement
Records when a word is used in a statement

##### Details
- Uses a **combo index**: `statement_id`

##### Properties
Word Placement contains the following 4 properties:
- **Statement Id** - `statementId: Int` - Id of the statement where the referenced word appears
  - Refers to [Statement](#statement)
- **Word Id** - `wordId: Int` - Id of the word that appears in the described statement
  - Refers to [Word](#word)
- **Order Index** - `orderIndex: Int`, `0` by default - 0-based index that indicates the specific location of the placed text
- **Style** - `style: DisplayStyle` - Style in which this word is used in this context

### Url
This package contains the following 4 classes: [Domain](#domain), [Link](#link), [Link Placement](#link-placement), [Request Path](#request-path)

#### Domain
Represents the address of an internet service

##### Details
- Uses **index**: `url`

##### Properties
Domain contains the following 2 properties:
- **Url** - `url: String` - Full http(s) address of this domain in string format. Includes protocol, domain name and possible port number.
- **Created** - `created: Instant` - Time when this domain was added to the database

##### Referenced from
- [Request Path](#request-path).`domainId`

#### Link
Represents a link for a specific http(s) request

##### Details

##### Properties
Link contains the following 3 properties:
- **Path Id** - `pathId: Int` - Id of the targeted internet address, including the specific sub-path
  - Refers to [Request Path](#request-path)
- **Query Parameters** - `queryParameters: Model` - Specified request parameters in model format
- **Created** - `created: Instant` - Time when this link was added to the database

##### Referenced from
- [Link Placement](#link-placement).`linkId`

#### Link Placement
Places a link within a statement

##### Details
- Uses a **combo index**: `statement_id`

##### Properties
Link Placement contains the following 3 properties:
- **Statement Id** - `statementId: Int` - Id of the statement where the specified link is referenced
  - Refers to [Statement](#statement)
- **Link Id** - `linkId: Int` - Referenced / placed link
  - Refers to [Link](#link)
- **Order Index** - `orderIndex: Int`, `0` by default - 0-based index that indicates the specific location of the placed text

#### Request Path
Represents a specific http(s) request url path part, not including any query parameters

##### Details
- Combines with [Domain](#domain), creating a **Detailed Request Path**
- Uses a **combo index**: `domain_id` => `path`

##### Properties
Request Path contains the following 3 properties:
- **Domain Id** - `domainId: Int` - Id of the domain part of this url
  - Refers to [Domain](#domain)
- **Path** - `path: String` - Part of this url that comes after the domain part. Doesn't include any query parameters, nor the initial forward slash.
- **Created** - `created: Instant` - Time when this request path was added to the database

##### Referenced from
- [Link](#link).`pathId`
