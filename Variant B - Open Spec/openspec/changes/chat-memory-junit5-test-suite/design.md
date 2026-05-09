## Context

The `spring-ai-model` module contains the ChatMemory subsystem: the `ChatMemory` interface, `MessageWindowChatMemory` (the only built-in implementation), `ChatMemoryRepository` interface, and `InMemoryChatMemoryRepository`. These classes are used throughout Spring AI to maintain conversational context. A focused, self-contained test file is needed to document and verify their behavior.

## Goals / Non-Goals

**Goals:**
- Produce exactly 30 compilable, passing JUnit 5 test methods in one file
- Cover the four classes: ChatMemory interface contract (via MessageWindowChatMemory), MessageWindowChatMemory window-truncation logic, InMemoryChatMemoryRepository CRUD, and ChatMemoryRepository abstraction
- Use AssertJ for assertions; Mockito only where it genuinely helps
- Test edge cases: empty state, window=1, large window, null/blank conversation IDs, overflow eviction, multiple independent conversations

**Non-Goals:**
- Changing production code
- Integration tests with Spring context
- Testing other ChatMemory implementations not present in this module

## Decisions

**Single test class, no inner classes**: Keeps the file simple and the 30 methods easy to count and audit.

**Direct instantiation over Spring context**: Tests use `new MessageWindowChatMemory(...)` and `new InMemoryChatMemoryRepository()` directly — no `@SpringBootTest`. This is a unit test file; fast and dependency-free.

**AssertJ exclusively for assertions**: The project already uses AssertJ; mixing JUnit 5 assertions would be inconsistent.

**Mockito only for ChatMemoryRepository contract tests**: A mock repository lets us verify that `MessageWindowChatMemory` calls the repository with the right arguments, without coupling to `InMemoryChatMemoryRepository` behavior.

**Test distribution (30 total)**:
- `InMemoryChatMemoryRepository` — ~8 tests (add, get, clear, isolation, empty, overwrite)
- `MessageWindowChatMemory` — ~14 tests (window overflow, ordering, multiple conversations, window=1, max window, clear, add-then-get)
- `ChatMemoryRepository` contract via mock — ~5 tests (add delegates, get delegates, clear delegates, correct conversationId)
- Edge / integration scenarios — ~3 tests (null-safe, large burst, re-use after clear)

## Risks / Trade-offs

[Risk: source API changes] → Tests are derived from reading source, so if the API changes the tests will fail rather than silently pass — acceptable, that's the point.

[Risk: 30-method count drifts] → The task explicitly calls for 30; the implementation task will count and adjust.
