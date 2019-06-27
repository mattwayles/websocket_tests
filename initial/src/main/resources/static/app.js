var stompClient = null;
let subscription = null;
const userId = localStorage.getItem("userId");


/**
 * Connect to the websocket server exposed on /ws
 */
function connect() {
    var socket = new SockJS('/wsConnection');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        //On successful connection...
        setConnected(true);
        console.log('Connected: ' + frame);

        //Subscribe to messages published to /topic/greetings
        subscription = stompClient.subscribe('/topic/greetings', function (greeting) {
            console.log("Client has received the following message from the server: ", greeting);
            //Every time a message is received on this channel, call showGreeting
            showGreeting(JSON.parse(greeting.body).content);

            console.log(greeting.body);
            //TESTING: Send an acknowledgment back to the server

            stompClient.send("/toServer/ack", {}, JSON.parse(greeting.body).messageId);
        }, {userId: userId});
    });
}

/**
 * Disconnect from the WebSocket server
 */
function disconnect() {
    stompClient.unsubscribe(subscription.id, {userId: userId});


    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

/**
 * Send the user input name to the server on the /toServer/fromClient channel
 */
function sendName() {
    stompClient.send("/toServer/fromClient", {}, JSON.stringify({'name': $("#name").val()}));
}


function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
        $("#connectionStatus").text("CONNECTED");
    }
    else {
        $("#conversation").hide();
        $("#connectionStatus").text("DISCONNECTED");
    }
    $("#greetings").html("");
}

function showGreeting(message) {
    $("#greetings").append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#connect" ).click(function() { connect(); });
    $( "#disconnect" ).click(function() { disconnect(); });
    $( "#send" ).click(function() { sendName(); });
});