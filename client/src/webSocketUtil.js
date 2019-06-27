import * as webstomp from "webstomp-client";

export const connectWsClient = (url, success, failure) => {
    let client = webstomp.over(new WebSocket(url));
    client.connect({}, () => success(client, url), failure);

    return client;
};