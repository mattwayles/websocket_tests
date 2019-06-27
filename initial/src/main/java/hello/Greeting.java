package hello;

import java.util.UUID;

public class Greeting {
    private String content;
    private String messageId = UUID.randomUUID().toString();

    public Greeting() {}

    public Greeting(String content) {
        this.content = content;
    }

    public String getContent() { return content; }
    public String getMessageId() { return messageId; }
}
