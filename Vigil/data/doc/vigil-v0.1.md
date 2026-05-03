# Vigil
Version: **v0.1**  
Updated: 2026-05-01

## Table of Contents
- [Enumerations](#enumerations)
  - [Scope Grant Type](#scope-grant-type)
- [Packages & Classes](#packages-and-classes)
  - [Scope](#scope)
    - [Scope](#scope)
    - [Scope Right](#scope-right)
  - [Token](#token)
    - [Token](#token)
    - [Token Grant Right](#token-grant-right)
    - [Token Scope](#token-scope)
    - [Token Template](#token-template)
    - [Token Template Scope](#token-template-scope)

## Enumerations
Below are listed all enumerations introduced in Vigil, in alphabetical order  

### Scope Grant Type
Enumeration for different ways scopes may be adjusted when generating new tokens

Key: `id: Int`  

**Values:**

Utilized by the following 1 classes:
- [Token Template](#token-template)

## Packages and Classes
Below are listed all classes introduced in Vigil, grouped by package and in alphabetical order.  
There are a total number of 2 packages and 7 classes

### Scope
This package contains the following 2 classes: [Scope](#scope), [Scope Right](#scope-right)

#### Scope
Used for limiting authorization to certain features or areas

##### Details
- Uses **index**: `key`

##### Properties
Scope contains the following 2 properties:
- **Key** - `key: String` - A key used for identifying this scope
- **Parent Id** - `parentId: Option[Int]` - ID of the scope that contains this scope. None if this is a root-level scope.
  - Refers to [Scope](#scope)

##### Referenced from
- [Scope](#scope).`parentId`
- [Scope Right](#scope-right).`scopeId`
- [Token Scope](#token-scope).`scopeId`
- [Token Template Scope](#token-template-scope).`scopeId`

#### Scope Right
Links a scope to an authentication method that grants that scope

##### Details
- Uses **index**: `usable`

##### Properties
Scope Right contains the following 3 properties:
- **Scope Id** - `scopeId: Int` - ID of the granted or accessible scope
  - Refers to [Scope](#scope)
- **Created** - `created: Instant` - Time when this scope right was added to the database
- **Usable** - `usable: Boolean` - Whether the linked scope is directly accessible. 
False if the scope is only applied when granting access for other authentication methods.

### Token
This package contains the following 5 classes: [Token](#token), [Token Grant Right](#token-grant-right), [Token Scope](#token-scope), [Token Template](#token-template), [Token Template Scope](#token-template-scope)

#### Token
Represents a token that may be used for authorizing certain actions

##### Details
- Fully **versioned**
- Uses a **combo index**: `revoked` => `expires`
- Uses 3 database **indices**: `hash`, `created`, `revoked`

##### Properties
Token contains the following 7 properties:
- **Template Id** - `templateId: Int` - ID of the template used when creating this token
  - Refers to [Token Template](#token-template)
- **Hash** - `hash: String` - Hashed version of this token
- **Parent Id** - `parentId: Option[Int]` - ID of the token that was used to generate this token
  - Refers to [Token](#token)
- **Name** - `name: String` - Name of this token. May be empty.
- **Created** - `created: Instant` - Time when this token was created
- **Expires** - `expires: Option[Instant]` - Time when this token automatically expires. None if this token doesn't expire automatically.
- **Revoked** - `revoked: Option[Instant]` - Time when this token was revoked.

##### Referenced from
- [Token](#token).`parentId`
- [Token Scope](#token-scope).`tokenId`

#### Token Grant Right
Used for allowing certain token types (templates) to generate new tokens of other types

##### Details

##### Properties
Token Grant Right contains the following 3 properties:
- **Owner Template Id** - `ownerTemplateId: Int` - ID of the token template that has been given the right to generate new tokens
  - Refers to [Token Template](#token-template)
- **Granted Template Id** - `grantedTemplateId: Int` - ID of the template applied to the generated tokens
  - Refers to [Token Template](#token-template)
- **Revokes** - `revokes: Boolean` - Whether generating a new token revokes the token used for authorizing that action

#### Token Scope
Allows a token to be used in some scope

##### Details
- Uses **index**: `usable`

##### Properties
Token Scope contains the following 4 properties:
- **Scope Id** - `scopeId: Int` - ID of the granted or accessible scope
  - Refers to [Scope](#scope)
- **Token Id** - `tokenId: Int` - ID of the token that grants or has access to the linked scope
  - Refers to [Token](#token)
- **Created** - `created: Instant` - Time when this scope right was added to the database
- **Usable** - `usable: Boolean` - Whether the linked scope is directly accessible. 
False if the scope is only applied when granting access for other authentication methods.

#### Token Template
A template or a mold for creating new tokens

##### Details
- Uses **index**: `name`

##### Properties
Token Template contains the following 4 properties:
- **Name** - `name: String` - Name of this template. May be empty.
- **Scope Grant Type** - `scopeGrantType: ScopeGrantType` - Way the scope-granting functions in this template
- **Duration** - `duration: Option[Duration]` - Duration of the created tokens. None if infinite.
- **Created** - `created: Instant` - Time when this token template was added to the database

##### Referenced from
- [Token](#token).`templateId`
- [Token Grant Right](#token-grant-right).`ownerTemplateId`
- [Token Grant Right](#token-grant-right).`grantedTemplateId`
- [Token Template Scope](#token-template-scope).`templateId`

#### Token Template Scope
Links a (granted) scope to a token template

##### Details
- Uses **index**: `usable`

##### Properties
Token Template Scope contains the following 4 properties:
- **Scope Id** - `scopeId: Int` - ID of the granted or accessible scope
  - Refers to [Scope](#scope)
- **Template Id** - `templateId: Int` - ID of the template that grants this scope
  - Refers to [Token Template](#token-template)
- **Created** - `created: Instant` - Time when this scope right was added to the database
- **Usable** - `usable: Boolean` - Whether the linked scope is directly accessible. 
False if the scope is only applied when granting access for other authentication methods.
