angular.module('myModule', ['ui.bootstrap'])

.controller('MyCtrl', function($scope) {
  $scope.messages = [];

  var loc = window.location, websocket_uri;
  if (loc.protocol === "https:") {
    websocket_uri = "wss:";
  }
  else {
    websocket_uri = "ws:";
  }
  websocket_uri += "//" + loc.host + loc.pathname;

  var ws = new WebSocket(websocket_uri);

  ws.onopen = function() {
    console.log("connection established ...");
  }

  ws.onmessage = function(event) {
    var data = JSON.parse(event.data);
    var username = data['username'];
    var message = data['contents'];

    $scope.$apply(function() {
      $scope.messages.push(username + ": " + message);
    });
  }

  ws.onclose = function(event) {
    console.log("Connection closed");
  }

  $scope.sendMessage = function() {
    var message = {
      type: "message",
      chat_room: "lobby",
      contents: $scope.enteredMessage,
    }

    ws.send(JSON.stringify(message));
    $scope.enteredMessage = "";
  }
});
