package no.kantega.polltibot.broker;

public class TopicName {
  public final String name;

  public TopicName(String name) {
    this.name = name;
  }



  public static TopicName gameevent(String sessionId) {
    return new TopicName("/ge/" + sessionId);
  }


}
