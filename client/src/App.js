import React from 'react';
import './App.css';
import {connectWsClient} from "./webSocketUtil";
import Auxil from "./Auxil";

class App extends React.Component {

    state = {
        client: null,
        subscription: false,
        connected: false
    };

  onConnect = () => {
      connectWsClient("ws://localhost:8080/wsConnection", this.onWsConnectionSuccess, this.onWsConnectionLoss);
  };

  onWsConnectionSuccess = (client) => {
      this.setState({ client: client, connected: true });
  };

  onWsConnectionLoss = () => {
      this.setState({ client: null, connected: false });
    console.log("LOST WEBSOCKET CONNECTION!");
  };



  handleSendMessage = () => {
    console.log("Sending message...");
    this.state.client.send("/toServer/fromClient", JSON.stringify({'name': 'Matt'}), {});
  };

  handleSubscribe = () => {
      if (!this.state.subscription) {
          //Connection to client successful, subscribe to the channel
          const sub = this.state.client.subscribe("/topic/greetings", (greeting) => {

              //Subscription successful, listening for events
              this.setMessage(JSON.parse(greeting.body).content);

              //Event received, send acknowledgment
              this.state.client.send("/toServer/ack", JSON.stringify({
                  subId: greeting.headers.subscription,
                  messageId: JSON.parse(greeting.body).messageId
              }), {});
          })
          this.setState({subscription: sub});
      }
  };

    handleUnsubscribe = () => {
        this.state.subscription.unsubscribe();
        this.setState({ subscription: null });
        //Connection to client successful, subscribe to the channel
        //client.subscribe("/topic/greetings", (greeting) => {

            //Subscription successful, listening for events
            //console.log("Client has received the following message from the server: ", greeting);

            //Event received, send acknowledgment
            //client.send("/toServer/ack", JSON.stringify({subId: greeting.headers.subscription, messageId: JSON.parse(greeting.body).messageId}), {});
        //})
    };

    setMessage = (message) => {
        this.setState({ message: message });
    };

  render() {
      const { connected, subscription, message } = this.state;

      return (
          <div className="App">
              <p style={{fontWeight: 900}}>Connection  Status:</p>
              <p>{connected ? "CONNECTED" : "DISCONNECTED"}</p>
              {connected ? <Auxil>
                  <button disabled={subscription} onClick={this.handleSubscribe}>Subscribe</button>
                  <button style={{margin: "0 25px"}} disabled={!subscription} onClick={this.handleUnsubscribe}>UnSubscribe</button>
                  <button onClick={this.handleSendMessage}>Send WebSocket Message</button>
              </Auxil> : <button style={{margin: "0 25px"}} onClick={this.onConnect}>Connect</button>}
              {message ? <p>{message}</p> : null}
          </div>
      );
  }
}

export default App;
