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

package VariantA;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Generated tests for the ChatMemory subsystem.
 */
@ExtendWith(MockitoExtension.class)
class GeneratedChatMemoryTest {

	// -------------------------------------------------------------------------
	// InMemoryChatMemoryRepository tests
	// -------------------------------------------------------------------------

	private InMemoryChatMemoryRepository repository;

	@BeforeEach
	void setUp() {
		this.repository = new InMemoryChatMemoryRepository();
	}

	@Test
	void inMemoryRepo_saveAndFind() {
		List<Message> messages = List.of(new UserMessage("hello"));
		this.repository.saveAll("conv1", messages);
		assertThat(this.repository.findByConversationId("conv1")).hasSize(1)
			.first()
			.extracting(Message::getText)
			.isEqualTo("hello");
	}

	@Test
	void inMemoryRepo_findReturnsEmptyListForUnknownConversation() {
		assertThat(this.repository.findByConversationId("unknown")).isEmpty();
	}

	@Test
	void inMemoryRepo_findReturnsCopy() {
		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("hello"));
		this.repository.saveAll("conv1", messages);
		List<Message> retrieved = this.repository.findByConversationId("conv1");
		retrieved.add(new UserMessage("extra"));
		assertThat(this.repository.findByConversationId("conv1")).hasSize(1);
	}

	@Test
	void inMemoryRepo_deleteByConversationId() {
		this.repository.saveAll("conv1", List.of(new UserMessage("hello")));
		this.repository.deleteByConversationId("conv1");
		assertThat(this.repository.findByConversationId("conv1")).isEmpty();
	}

	@Test
	void inMemoryRepo_deleteNonExistentConversationDoesNotThrow() {
		this.repository.deleteByConversationId("nonexistent");
		assertThat(this.repository.findConversationIds()).doesNotContain("nonexistent");
	}

	@Test
	void inMemoryRepo_findConversationIds() {
		this.repository.saveAll("conv1", List.of(new UserMessage("a")));
		this.repository.saveAll("conv2", List.of(new UserMessage("b")));
		assertThat(this.repository.findConversationIds()).containsExactlyInAnyOrder("conv1", "conv2");
	}

	@Test
	void inMemoryRepo_saveAllReplacesExistingMessages() {
		this.repository.saveAll("conv1", List.of(new UserMessage("first")));
		this.repository.saveAll("conv1", List.of(new UserMessage("second"), new UserMessage("third")));
		List<Message> messages = this.repository.findByConversationId("conv1");
		assertThat(messages).hasSize(2);
		assertThat(messages.get(0).getText()).isEqualTo("second");
	}

	@Test
	void inMemoryRepo_saveAllNullConversationIdThrows() {
		assertThatThrownBy(() -> this.repository.saveAll(null, List.of(new UserMessage("hi"))))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void inMemoryRepo_saveAllNullMessagesListThrows() {
		assertThatThrownBy(() -> this.repository.saveAll("conv1", null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void inMemoryRepo_findByNullConversationIdThrows() {
		assertThatThrownBy(() -> this.repository.findByConversationId(null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void inMemoryRepo_deleteByNullConversationIdThrows() {
		assertThatThrownBy(() -> this.repository.deleteByConversationId(null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void inMemoryRepo_conversationIsolation() {
		this.repository.saveAll("conv1", List.of(new UserMessage("conv1-msg")));
		this.repository.saveAll("conv2", List.of(new AssistantMessage("conv2-msg")));
		assertThat(this.repository.findByConversationId("conv1")).hasSize(1)
			.first()
			.extracting(Message::getText)
			.isEqualTo("conv1-msg");
		assertThat(this.repository.findByConversationId("conv2")).hasSize(1)
			.first()
			.extracting(Message::getText)
			.isEqualTo("conv2-msg");
	}

	// -------------------------------------------------------------------------
	// MessageWindowChatMemory tests
	// -------------------------------------------------------------------------

	@Test
	void messageWindowMemory_defaultMaxMessagesIs20() {
		InMemoryChatMemoryRepository repo = new InMemoryChatMemoryRepository();
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder().chatMemoryRepository(repo).build();
		for (int i = 0; i < 25; i++) {
			memory.add("conv", new UserMessage("msg-" + i));
		}
		assertThat(memory.get("conv")).hasSize(20);
	}

	@Test
	void messageWindowMemory_customMaxMessages() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(5)
			.build();
		for (int i = 0; i < 8; i++) {
			memory.add("conv", new UserMessage("msg-" + i));
		}
		assertThat(memory.get("conv")).hasSize(5);
	}

	@Test
	void messageWindowMemory_windowKeepsNewestMessages() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(3)
			.build();
		memory.add("conv", new UserMessage("oldest"));
		memory.add("conv", new UserMessage("middle"));
		memory.add("conv", new UserMessage("newest"));
		memory.add("conv", new UserMessage("extra"));
		List<Message> messages = memory.get("conv");
		assertThat(messages).hasSize(3);
		assertThat(messages.get(messages.size() - 1).getText()).isEqualTo("extra");
		assertThat(messages).noneMatch(m -> m.getText().equals("oldest"));
	}

	@Test
	void messageWindowMemory_systemMessagePreservedOnOverflow() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(3)
			.build();
		memory.add("conv", new SystemMessage("system-prompt"));
		memory.add("conv", new UserMessage("user1"));
		memory.add("conv", new AssistantMessage("assistant1"));
		memory.add("conv", new UserMessage("user2"));
		List<Message> messages = memory.get("conv");
		assertThat(messages).hasSize(3);
		assertThat(messages).anyMatch(m -> m instanceof SystemMessage);
	}

	@Test
	void messageWindowMemory_newSystemMessageReplacesOldOne() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(10)
			.build();
		memory.add("conv", new SystemMessage("old-system"));
		memory.add("conv", new UserMessage("user1"));
		memory.add("conv", new SystemMessage("new-system"));
		List<Message> messages = memory.get("conv");
		long systemCount = messages.stream().filter(m -> m instanceof SystemMessage).count();
		assertThat(systemCount).isEqualTo(1);
		assertThat(messages).anyMatch(m -> m instanceof SystemMessage && m.getText().equals("new-system"));
		assertThat(messages).noneMatch(m -> m instanceof SystemMessage && m.getText().equals("old-system"));
	}

	@Test
	void messageWindowMemory_getEmptyConversation() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.build();
		assertThat(memory.get("nonexistent")).isEmpty();
	}

	@Test
	void messageWindowMemory_clearConversation() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.build();
		memory.add("conv", new UserMessage("hello"));
		memory.clear("conv");
		assertThat(memory.get("conv")).isEmpty();
	}

	@Test
	void messageWindowMemory_clearNonExistentConversationDoesNotThrow() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.build();
		memory.clear("nonexistent");
	}

	@Test
	void messageWindowMemory_addNullConversationIdThrows() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.build();
		assertThatThrownBy(() -> memory.add(null, List.of(new UserMessage("hi"))))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void messageWindowMemory_addNullMessageThrows() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.build();
		assertThatThrownBy(() -> memory.add("conv", (Message) null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void messageWindowMemory_addNullMessagesListThrows() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.build();
		assertThatThrownBy(() -> memory.add("conv", (List<Message>) null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void messageWindowMemory_addBatchOfMessages() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(10)
			.build();
		memory.add("conv", List.of(new UserMessage("u1"), new AssistantMessage("a1"), new UserMessage("u2")));
		assertThat(memory.get("conv")).hasSize(3);
	}

	@Test
	void messageWindowMemory_conversationIsolation() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.build();
		memory.add("conv1", new UserMessage("conv1-msg"));
		memory.add("conv2", new UserMessage("conv2-msg"));
		assertThat(memory.get("conv1")).hasSize(1).first().extracting(Message::getText).isEqualTo("conv1-msg");
		assertThat(memory.get("conv2")).hasSize(1).first().extracting(Message::getText).isEqualTo("conv2-msg");
	}

	@Test
	void messageWindowMemory_builderDefaultsToInMemoryRepo() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder().build();
		memory.add("conv", new UserMessage("hello"));
		assertThat(memory.get("conv")).hasSize(1);
	}

	@Test
	void messageWindowMemory_maxMessagesZeroThrows() {
		assertThatThrownBy(() -> MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(0)
			.build()).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void messageWindowMemory_messageOrderPreserved() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(10)
			.build();
		memory.add("conv", new UserMessage("first"));
		memory.add("conv", new AssistantMessage("second"));
		memory.add("conv", new UserMessage("third"));
		List<Message> messages = memory.get("conv");
		assertThat(messages).hasSize(3);
		assertThat(messages.get(0).getText()).isEqualTo("first");
		assertThat(messages.get(1).getText()).isEqualTo("second");
		assertThat(messages.get(2).getText()).isEqualTo("third");
	}

	// -------------------------------------------------------------------------
	// ChatMemoryRepository contract compliance via mock
	// -------------------------------------------------------------------------

	@Mock
	private ChatMemoryRepository mockRepository;

	@Test
	void messageWindowMemory_delegatesFindToRepository() {
		given(this.mockRepository.findByConversationId("conv1")).willReturn(List.of(new UserMessage("stored")));
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(this.mockRepository)
			.build();
		List<Message> result = memory.get("conv1");
		assertThat(result).hasSize(1);
		verify(this.mockRepository).findByConversationId("conv1");
	}

	@Test
	void messageWindowMemory_delegatesSaveAllToRepository() {
		given(this.mockRepository.findByConversationId("conv1")).willReturn(List.of());
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(this.mockRepository)
			.build();
		memory.add("conv1", new UserMessage("hi"));
		verify(this.mockRepository).saveAll(eq("conv1"), anyList());
	}

	@Test
	void messageWindowMemory_delegatesDeleteToRepository() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(this.mockRepository)
			.build();
		memory.clear("conv1");
		verify(this.mockRepository).deleteByConversationId("conv1");
	}

	@Test
	void messageWindowMemory_getNullConversationIdThrows() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.build();
		assertThatThrownBy(() -> memory.get(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void messageWindowMemory_clearNullConversationIdThrows() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.build();
		assertThatThrownBy(() -> memory.clear(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void messageWindowMemory_exactlyAtMaxDoesNotTrim() {
		MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
			.chatMemoryRepository(new InMemoryChatMemoryRepository())
			.maxMessages(3)
			.build();
		memory.add("conv", List.of(new UserMessage("u1"), new AssistantMessage("a1"), new UserMessage("u2")));
		assertThat(memory.get("conv")).hasSize(3);
	}

	@Test
	void inMemoryRepo_saveAllWithNullElementInListThrows() {
		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("valid"));
		messages.add(null);
		assertThatThrownBy(() -> this.repository.saveAll("conv1", messages))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
