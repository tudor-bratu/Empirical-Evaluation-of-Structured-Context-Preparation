## 1. Research Source Code

- [x] 1.1 Read ChatMemory.java interface to understand the add/get/clear API signatures
- [x] 1.2 Read MessageWindowChatMemory.java to understand window logic and constructor options
- [x] 1.3 Read ChatMemoryRepository.java interface for contract details
- [x] 1.4 Read InMemoryChatMemoryRepository.java for storage behavior

## 2. Write Test File

- [x] 2.1 Create GeneratedChatMemoryTest.java at spring-ai-model/src/test/java/org/springframework/ai/chat/memory/ with package declaration and all imports
- [x] 2.2 Add 8 tests for InMemoryChatMemoryRepository (add/get/clear/isolation/empty/overwrite/multi-type/re-add-after-clear)
- [x] 2.3 Add 14 tests for MessageWindowChatMemory (window overflow, ordering, window=1, exact boundary, default window, clear, multiple conversations, large burst, per-conversation isolation, after-clear reuse, add-then-get, single message)
- [x] 2.4 Add 5 tests using Mockito mock of ChatMemoryRepository (add/get/clear delegation, correct conversationId on add/clear)
- [x] 2.5 Add 3 edge-case tests (get on never-used conversation, clear non-existent is no-op, single message round-trip)
- [x] 2.6 Verify test count is exactly 30

## 3. Compile and Fix

- [x] 3.1 Compile the test file and resolve any import or API errors
- [x] 3.2 Run the 30 tests and fix any assertion errors caused by wrong test logic
- [x] 3.3 Confirm all 30 tests pass
