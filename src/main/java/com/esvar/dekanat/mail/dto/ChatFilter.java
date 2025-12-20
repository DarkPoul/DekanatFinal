package com.esvar.dekanat.mail.dto;

import com.esvar.dekanat.mail.ChatStatus;
import lombok.Data;

import java.util.List;

@Data
public class ChatFilter {
    private String query;
    private List<ChatStatus> statuses;
}
