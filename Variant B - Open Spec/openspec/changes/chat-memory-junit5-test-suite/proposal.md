## Why

The ChatMemory subsystem (ChatMemory interface, MessageWindowChatMemory, InMemoryChatMemoryRepository) lacks a dedicated JUnit 5 test suite, leaving critical behaviors like message-window overflow, conversation isolation, and repository contract compliance untested in an explicit, readable form.

## What Changes

- Add `GeneratedChatMemoryTest.java` with exactly 30 JUnit 5 test methods covering the ChatMemory subsystem
- Tests live in `spring-ai-model/src/test/java/org/springframework/ai/chat/memory/`
- No production code changes; test-only addition

## Capabilities

### New Capabilities
- `chat-memory-test-suite`: 30 JUnit 5 tests covering ChatMemory interface contract, MessageWindowChatMemory window behavior, InMemoryChatMemoryRepository CRUD, conversation isolation, message ordering, and edge cases (empty conversations, null inputs, window overflow, ID collisions)

### Modified Capabilities

## Impact

- Affected module: `spring-ai-model`
- No API or production-code changes
- Requires AssertJ and Mockito (already on the test classpath)
