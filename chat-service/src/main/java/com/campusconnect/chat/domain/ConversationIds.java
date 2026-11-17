package com.campusconnect.chat.domain;

public final class ConversationIds {

    private ConversationIds() {}

    public static String of(Long userA, Long userB) {
        long min = Math.min(userA, userB);
        long max = Math.max(userA, userB);
        return min + "_" + max;
    }
}
