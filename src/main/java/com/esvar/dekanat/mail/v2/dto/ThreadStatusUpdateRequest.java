package com.esvar.dekanat.mail.v2.dto;

import com.esvar.dekanat.mail.v2.entity.MailThreadEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ThreadStatusUpdateRequest {
    private MailThreadEntity.ThreadStatus status;
}
