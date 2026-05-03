package com.splitmoney.ai.ai;

import com.splitmoney.ai.conversation.ConversationState.ChatMessage;

import java.util.List;

public interface AiChatProvider {
    String chat(List<ChatMessage> history, String systemPrompt);

    /**
     * Multimodal variant: sends the last user message together with a file attachment.
     * Providers that support vision/documents override this; the default falls back to text-only.
     *
     * @param fileBytes raw bytes of the attached file
     * @param mimeType  MIME type (e.g. "image/jpeg", "application/pdf")
     */
    default String chatWithDocument(List<ChatMessage> history, String systemPrompt,
                                    byte[] fileBytes, String mimeType) {
        return chat(history, systemPrompt);
    }

    String providerName();
}
