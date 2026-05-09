/*
 * Copyright 2023-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.chat.memory;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Generated JUnit 5 test suite for the ChatMemory subsystem. Covers ChatMemory interface
 * contract, MessageWindowChatMemory window behavior, InMemoryChatMemoryRepository CRUD,
 * and ChatMemoryRepository delegation.
 */
@ExtendWith(MockitoExtension.class)
class GeneratedChatMemoryTest {

	@Mock
	private ChatMemoryRepository mockRepository;

	private InMemoryChatMemoryRepository repository;

	private MessageWindowChatMemory memory;

	@BeforeEach
	void setUp() {
		this.repository = new InMemoryChatMemoryRepository();
		this.memory = MessageWindowChatMemory.builder().chatMemoryRepository(this.repository).maxMessages(5).build();
	}

	// ── InMemoryChatMemoryRepository (8 tests) ────────────────────────────────

	@Test
	void repositorySaveAndFind_returnsMessages() {
		List<Message> messages = List.of(new UserMessage("hello"), new AssistantMessage("hi"));
		this.repository.saveAll("conv-1", messages);
		assertThat(this.repository.findByConversationId("conv-1")).containsExactlyElementsOf(messages);
	}

	@Test
	void repositoryFindByConversationId_emptyWhenNotFound() {
		assertThat(this.repository.findByConversationId("unknown-conv")).isEmpty();
	}

	@Test
	void repositoryDeleteByConversationId_clearsMessages() {
		this.repository.saveAll("conv-2", List.of(new UserMessage("msg")));
		this.repository.deleteByConversationId("conv-2");
		assertThat(this.repository.findByConversationId("conv-2")).isEmpty();
	}

	@Test
	void repositoryConversations_areIsolated() {
		UserMessage msgA = new UserMessage("message for A");
		UserMessage msgB = new UserMessage("message for B");
		this.repository.saveAll("conv-A", List.of(msgA));
		this.repository.saveAll("conv-B", List.of(msgB));
		assertThat(this.repository.findByConversationId("conv-A")).containsExactly(msgA);
		assertThat(this.repository.findByConversationId("conv-B")).containsExactly(msgB);
	}

	@Test
	void repositorySaveAll_replacesExistingMessages() {
		this.repository.saveAll("conv-3", List.of(new UserMessage("original")));
		List<Message> replacement = List.of(new UserMessage("replacement"));
		this.repository.saveAll("conv-3", replacement);
		assertThat(this.repository.findByConversationId("conv-3")).containsExactlyElementsOf(replacement);
	}

	@Test
	void repositoryDelete_doesNotAffectOtherConversation() {
		UserMessage msgB = new UserMessage("keep me");
		this.repository.saveAll("conv-del", List.of(new UserMessage("gone")));
		this.repository.saveAll("conv-keep", List.of(msgB));
		this.repository.deleteByConversationId("conv-del");
		assertThat(this.repository.findByConversationId("conv-keep")).containsExactly(msgB);
	}

	@Test
	void repositoryFindConversationIds_returnsAllIds() {
		this.repository.saveAll("id-1", List.of(new UserMessage("a")));
		this.repository.saveAll("id-2", List.of(new UserMessage("b")));
		this.repository.saveAll("id-3", List.of(new UserMessage("c")));
		assertThat(this.repository.findConversationIds()).containsExactlyInAnyOrder("id-1", "id-2", "id-3");
	}

	@Test
	void repositoryReAddAfterDelete_returnsOnlyNewMessages() {
		this.repository.saveAll("conv-reuse", List.of(new UserMessage("old")));
		this.repository.deleteByConversationId("conv-reuse");
		UserMessage newMsg = new UserMessage("new");
		this.repository.saveAll("conv-reuse", List.of(newMsg));
		assertThat(this.repository.findByConversationId("conv-reuse")).containsExactly(newMsg);
	}

	// ── MessageWindowChatMemory (14 tests) ────────────────────────────────────

	@Test
	void memoryAdd_returnsAllMessagesWhenUnderWindow() {
		this.memory.add("c1", List.of(new UserMessage("u1"), new UserMessage("u2"), new UserMessage("u3")));
		assertThat(this.memory.get("c1")).hasSize(3);
	}

	@Test
	void memoryAdd_evictsOldestWhenWindowExceeded() {
		List<Message> msgs = new ArrayList<>();
		for (int i = 1; i <= 7; i++) {
			msgs.add(new UserMessage("msg-" + i));
		}
		this.memory.add("c2", msgs);
		List<Message> result = this.memory.get("c2");
		assertThat(result).hasSize(5);
		assertThat(((UserMessage) result.get(0)).getText()).isEqualTo("msg-3");
		assertThat(((UserMessage) result.get(4)).getText()).isEqualTo("msg-7");
	}

	@Test
	void memoryWindowOfOne_keepsOnlyLastMessage() {
		MessageWindowChatMemory win1 = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(1)
			.build();
		win1.add("c3", List.of(new UserMessage("first"), new UserMessage("second"), new UserMessage("third")));
		List<Message> result = win1.get("c3");
		assertThat(result).hasSize(1);
		assertThat(((UserMessage) result.get(0)).getText()).isEqualTo("third");
	}

	@Test
	void memoryExactWindowBoundary_keepsAllMessages() {
		List<Message> msgs = List.of(new UserMessage("a"), new UserMessage("b"), new UserMessage("c"),
				new UserMessage("d"), new UserMessage("e"));
		this.memory.add("c4", msgs);
		assertThat(this.memory.get("c4")).hasSize(5);
	}

	@Test
	void memoryDefaultWindow_allows20Messages() {
		MessageWindowChatMemory defaultMemory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.build();
		List<Message> msgs = new ArrayList<>();
		for (int i = 1; i <= 20; i++) {
			msgs.add(new UserMessage("msg-" + i));
		}
		defaultMemory.add("c5", msgs);
		assertThat(defaultMemory.get("c5")).hasSize(20);
	}

	@Test
	void memoryClear_emptiesConversation() {
		this.memory.add("c6", List.of(new UserMessage("hello")));
		this.memory.clear("c6");
		assertThat(this.memory.get("c6")).isEmpty();
	}

	@Test
	void memoryMultipleConversations_areIsolated() {
		this.memory.add("conv-x", List.of(new UserMessage("x-msg")));
		this.memory.add("conv-y", List.of(new UserMessage("y-msg")));
		assertThat(this.memory.get("conv-x")).hasSize(1);
		assertThat(this.memory.get("conv-y")).hasSize(1);
		assertThat(((UserMessage) this.memory.get("conv-x").get(0)).getText()).isEqualTo("x-msg");
		assertThat(((UserMessage) this.memory.get("conv-y").get(0)).getText()).isEqualTo("y-msg");
	}

	@Test
	void memoryLargeBurst_windowedToMaxMessages() {
		MessageWindowChatMemory win10 = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(10)
			.build();
		List<Message> msgs = new ArrayList<>();
		for (int i = 1; i <= 100; i++) {
			msgs.add(new UserMessage("burst-" + i));
		}
		win10.add("c7", msgs);
		assertThat(win10.get("c7")).hasSize(10);
		assertThat(((UserMessage) win10.get("c7").get(9)).getText()).isEqualTo("burst-100");
	}

	@Test
	void memoryWindow_isPerConversationNotGlobal() {
		List<Message> overflow = new ArrayList<>();
		for (int i = 1; i <= 7; i++) {
			overflow.add(new UserMessage("o-" + i));
		}
		this.memory.add("conv-overflow", overflow);
		this.memory.add("conv-small", List.of(new UserMessage("s1"), new UserMessage("s2")));
		assertThat(this.memory.get("conv-overflow")).hasSize(5);
		assertThat(this.memory.get("conv-small")).hasSize(2);
	}

	@Test
	void memoryClearThenRefill_resetsWindow() {
		this.memory.add("c8", List.of(new UserMessage("old1"), new UserMessage("old2")));
		this.memory.clear("c8");
		List<Message> fresh = List.of(new UserMessage("n1"), new UserMessage("n2"), new UserMessage("n3"));
		this.memory.add("c8", fresh);
		assertThat(this.memory.get("c8")).hasSize(3);
		assertThat(((UserMessage) this.memory.get("c8").get(0)).getText()).isEqualTo("n1");
	}

	@Test
	void memoryAddThenGet_reflectsAddedMessages() {
		UserMessage u1 = new UserMessage("question");
		AssistantMessage a1 = new AssistantMessage("answer");
		this.memory.add("c9", List.of(u1, a1));
		List<Message> result = this.memory.get("c9");
		assertThat(result).hasSize(2);
		assertThat(result.get(0)).isInstanceOf(UserMessage.class);
		assertThat(result.get(1)).isInstanceOf(AssistantMessage.class);
	}

	@Test
	void memorySingleMessage_returnedCorrectly() {
		UserMessage single = new UserMessage("only one");
		this.memory.add("c10", single);
		assertThat(this.memory.get("c10")).containsExactly(single);
	}

	@Test
	void memorySystemMessage_preservedDuringEviction() {
		MessageWindowChatMemory win5 = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(5)
			.build();
		SystemMessage sys = new SystemMessage("be helpful");
		List<Message> msgs = new ArrayList<>();
		msgs.add(sys);
		for (int i = 1; i <= 6; i++) {
			msgs.add(new UserMessage("u" + i));
		}
		win5.add("c11", msgs);
		List<Message> result = win5.get("c11");
		assertThat(result).hasSize(5);
		assertThat(result.get(0)).isInstanceOf(SystemMessage.class);
	}

	@Test
	void memoryNewSystemMessage_replacesOldSystemMessage() {
		MessageWindowChatMemory win10 = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(10)
			.build();
		win10.add("c12", new SystemMessage("old instruction"));
		win10.add("c12", new SystemMessage("new instruction"));
		List<Message> result = win10.get("c12");
		long systemCount = result.stream().filter(SystemMessage.class::isInstance).count();
		assertThat(systemCount).isEqualTo(1);
		assertThat(
				((SystemMessage) result.stream().filter(SystemMessage.class::isInstance).findFirst().get()).getText())
			.isEqualTo("new instruction");
	}

	// ── ChatMemoryRepository delegation via Mockito (5 tests) ─────────────────

	@Test
	void mock_add_callsFindByConversationId() {
		when(this.mockRepository.findByConversationId("mock-conv")).thenReturn(List.of());
		MessageWindowChatMemory mockMemory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(this.mockRepository)
			.maxMessages(5)
			.build();
		mockMemory.add("mock-conv", List.of(new UserMessage("hi")));
		verify(this.mockRepository).findByConversationId("mock-conv");
	}

	@Test
	void mock_add_callsSaveAll() {
		when(this.mockRepository.findByConversationId("mock-conv")).thenReturn(List.of());
		MessageWindowChatMemory mockMemory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(this.mockRepository)
			.maxMessages(5)
			.build();
		mockMemory.add("mock-conv", List.of(new UserMessage("hi")));
		verify(this.mockRepository).saveAll(eq("mock-conv"), anyList());
	}

	@Test
	void mock_get_callsFindByConversationId() {
		when(this.mockRepository.findByConversationId("mock-conv")).thenReturn(List.of());
		MessageWindowChatMemory mockMemory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(this.mockRepository)
			.maxMessages(5)
			.build();
		mockMemory.get("mock-conv");
		verify(this.mockRepository).findByConversationId("mock-conv");
	}

	@Test
	void mock_clear_callsDeleteByConversationId() {
		MessageWindowChatMemory mockMemory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(this.mockRepository)
			.maxMessages(5)
			.build();
		mockMemory.clear("mock-conv");
		verify(this.mockRepository).deleteByConversationId("mock-conv");
	}

	@Test
	void mock_add_passesCorrectConversationId() {
		when(this.mockRepository.findByConversationId("conv-42")).thenReturn(List.of());
		MessageWindowChatMemory mockMemory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(this.mockRepository)
			.maxMessages(5)
			.build();
		mockMemory.add("conv-42", List.of(new UserMessage("test")));
		verify(this.mockRepository).findByConversationId("conv-42");
		verify(this.mockRepository).saveAll(eq("conv-42"), anyList());
	}

	// ── Edge cases (3 tests) ──────────────────────────────────────────────────

	@Test
	void memory_getNeverUsedConversation_returnsEmptyList() {
		List<Message> result = this.memory.get("never-used");
		assertThat(result).isNotNull().isEmpty();
	}

	@Test
	void memory_clearNonExistentConversation_doesNotThrow() {
		assertThatNoException().isThrownBy(() -> this.memory.clear("does-not-exist"));
	}

	@Test
	void repository_saveEmptyList_storesEmptyMessages() {
		this.repository.saveAll("conv-empty", List.of(new UserMessage("initial")));
		this.repository.saveAll("conv-empty", List.of());
		assertThat(this.repository.findByConversationId("conv-empty")).isEmpty();
	}

}
