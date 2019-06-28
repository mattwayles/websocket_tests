package hello;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class GreetingController {
    private Map<String, List<String>> unacknowledgedMessages = new ConcurrentHashMap<>();
    private ObjectMapper mapper = new ObjectMapper();
    private List<String> subscribers = new ArrayList<>();


    /**
     * Event listener to register subscribers in the receipts map
     * @param event The Subscribe event that exposes the subscriber information
     */
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        //Retrieve the user ID for this subscription from the custom header
        this.subscribers.add(event.getMessage().getHeaders().get("simpSubscriptionId").toString());
        System.out.println("Added subscription " + event.getMessage().getHeaders().get("simpSubscriptionId").toString());
    }

    /**
     * Event listener to register subscribers in the receipts map
     * @param event The Subscribe event that exposes the subscriber information
     */
    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        //Retrieve the user ID for this subscription from the custom header
        this.subscribers.remove(event.getMessage().getHeaders().get("simpSubscriptionId").toString());
        System.out.println("Removed subscription " + event.getMessage().getHeaders().get("simpSubscriptionId").toString());
    }



    //Ensure that if a message is sent to /fromClient, this function is called
    @MessageMapping("/fromClient")

    //Send the output to all subscribers on the /topic/greetings channel
    @SendTo("/topic/greetings")

    //Receive a HelloMessage and generate a Greeting
    public Greeting greeting(HelloMessage message) {
        Greeting greeting = new Greeting("Everything Worked");
        unacknowledgedMessages.put(greeting.getMessageId(), new ArrayList<>(subscribers));
        System.out.println("Server has sent message " + greeting.getMessageId() + " to " + subscribers.size() +
                " subscribers. Awaiting receipt...");

            Runnable r = () -> {
                try {
                    Thread.sleep(5000);
                    if (unacknowledgedMessages.containsKey(greeting.getMessageId())) {
                        boolean multiFail = unacknowledgedMessages.get(greeting.getMessageId()).size() > 1;
                        System.err.println("Never received acknowledgment of receipt of message " + greeting.getMessageId() +
                                " from subscriber" + (multiFail ? "s " : " ") + unacknowledgedMessages.get(greeting.getMessageId()) +
                                ". Attempting to resend...");
                        //TODO: Attempt to resend only to missing nodes
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };

            new Thread(r).start();
        return greeting;
    }


    //Ensure that if a message is sent to /ack, this function is called
    @MessageMapping("/ack")

    //Receive a HelloMessage and generate a Greeting
    public void acknowledge(String clientAck) {
        try {

            //Create an Acknowledgement containing the meddage ID and subscription ID
            Acknowledgment clientAckObj = mapper.readValue(clientAck, Acknowledgment.class);

            System.out.print("Server has received acknowledgement for message ID " + clientAckObj.getMessageId() + " from client " + clientAckObj.getSubId() + ".");

            //Check if any outstanding acknowledgments remain for this message
            if (unacknowledgedMessages.containsKey(clientAckObj.getMessageId())) {
                if (unacknowledgedMessages.get(clientAckObj.getMessageId()).size() == 1) {
                    System.out.println();
                    System.out.println("All clients have acknowledged " + clientAckObj.getMessageId() + ". Removing from queue");

                    //If no outstanding acknowledgments remain, remove the message from the map completely
                    unacknowledgedMessages.remove(clientAckObj.getMessageId());
                } else {
                    //Remove the subscription ID from this message in the unacknowledgedMessages map
                    unacknowledgedMessages.get(clientAckObj.getMessageId()).remove(clientAckObj.getSubId());
                    System.out.println(" Still awaiting receipt from " + unacknowledgedMessages.get(clientAckObj.getMessageId()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
