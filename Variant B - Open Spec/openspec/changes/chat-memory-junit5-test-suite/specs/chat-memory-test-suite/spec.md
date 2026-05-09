## ADDED Requirements

### Requirement: InMemoryChatMemoryRepository stores and retrieves messages per conversation
The repository SHALL store messages keyed by conversationId and return them in insertion order.

#### Scenario: Add and retrieve messages
- **WHEN** messages are added for a conversationId
- **THEN** `get(conversationId)` returns all messages in insertion order

#### Scenario: Empty conversation returns empty list
- **WHEN** `get` is called for a conversationId with no messages
- **THEN** an empty list is returned

#### Scenario: Clear removes all messages for a conversation
- **WHEN** `clear(conversationId)` is called
- **THEN** subsequent `get(conversationId)` returns an empty list

#### Scenario: Conversations are isolated
- **WHEN** messages are added to two different conversationIds
- **THEN** each `get` returns only its own messages

#### Scenario: Adding messages to same conversation accumulates them
- **WHEN** `add` is called multiple times for the same conversationId
- **THEN** `get` returns all added messages in order

#### Scenario: Clear does not affect other conversations
- **WHEN** `clear` is called for conversationId A
- **THEN** `get` for conversationId B still returns its messages

#### Scenario: Re-adding after clear starts fresh
- **WHEN** a conversation is cleared and new messages are added
- **THEN** only the new messages are returned

#### Scenario: Multiple message types stored correctly
- **WHEN** UserMessage, AssistantMessage, and SystemMessage are added
- **THEN** all are returned in the correct order

### Requirement: MessageWindowChatMemory enforces a maximum message window
MessageWindowChatMemory SHALL retain only the most recent N messages (where N is the configured window size), evicting oldest messages first when the window is exceeded.

#### Scenario: Messages within window are all retained
- **WHEN** fewer messages than the window size are added
- **THEN** all messages are returned

#### Scenario: Messages exceeding window are truncated to window size
- **WHEN** more messages than the window size are added
- **THEN** only the most recent window-size messages are returned

#### Scenario: Window of 1 retains only the last message
- **WHEN** window size is 1 and multiple messages are added
- **THEN** only the last message is returned

#### Scenario: Exact window boundary retains all messages
- **WHEN** exactly window-size messages are added
- **THEN** all window-size messages are returned

#### Scenario: Messages returned in chronological order
- **WHEN** messages are added sequentially
- **THEN** `get` returns them oldest-first

#### Scenario: Add then get reflects new messages
- **WHEN** messages are added and then retrieved
- **THEN** the retrieved list contains all added messages (up to window)

#### Scenario: Clear empties the conversation
- **WHEN** `clear(conversationId)` is called on MessageWindowChatMemory
- **THEN** subsequent `get` returns an empty list

#### Scenario: Multiple conversations are independent
- **WHEN** messages are added to two different conversationIds
- **THEN** each conversation's window is managed independently

#### Scenario: Large burst adds are windowed correctly
- **WHEN** 100 messages are added with window=10
- **THEN** only the last 10 messages are returned

#### Scenario: Window applies per-conversation not globally
- **WHEN** conversation A overflows and conversation B has fewer messages
- **THEN** conversation B messages are unaffected

#### Scenario: After clear, window resets for re-use
- **WHEN** a conversation is cleared and refilled to window capacity
- **THEN** all new messages are returned without truncation

#### Scenario: Default window size is applied when not specified
- **WHEN** MessageWindowChatMemory is created without explicit window size
- **THEN** the default window size is used

### Requirement: MessageWindowChatMemory delegates to ChatMemoryRepository
MessageWindowChatMemory SHALL delegate add/get/clear operations to the injected ChatMemoryRepository with the correct conversationId.

#### Scenario: add delegates to repository
- **WHEN** `add(conversationId, messages)` is called
- **THEN** the repository receives `add(conversationId, messages)`

#### Scenario: get delegates to repository
- **WHEN** `get(conversationId, lastN)` is called
- **THEN** the repository's `get(conversationId)` is invoked

#### Scenario: clear delegates to repository
- **WHEN** `clear(conversationId)` is called
- **THEN** the repository's `clear(conversationId)` is invoked

#### Scenario: correct conversationId is passed on add
- **WHEN** `add` is called with conversationId "conv-42"
- **THEN** the repository receives exactly "conv-42" as the conversationId

#### Scenario: correct conversationId is passed on clear
- **WHEN** `clear` is called with conversationId "conv-42"
- **THEN** the repository's `clear` is called with exactly "conv-42"

### Requirement: ChatMemory edge cases are handled gracefully
The system SHALL handle edge cases without throwing unexpected exceptions.

#### Scenario: Getting messages from never-used conversation returns empty
- **WHEN** `get` is called for a conversationId that was never written to
- **THEN** an empty list is returned, not null

#### Scenario: Clearing a non-existent conversation is a no-op
- **WHEN** `clear` is called for a conversationId with no messages
- **THEN** no exception is thrown and subsequent `get` returns empty

#### Scenario: Adding single message then retrieving works correctly
- **WHEN** a single message is added
- **THEN** `get` returns a list with exactly that one message
