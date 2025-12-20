package com.esvar.dekanat.mail;

import com.vaadin.flow.component.html.Div;

public class MessageDateDivider extends Div {

    public MessageDateDivider(String label) {
        addClassName("message-date-divider");
        setText(label);
    }
}
