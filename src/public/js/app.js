angular.module('myModule', ['ui.bootstrap'])

.controller('MyCtrl', function($scope) {
  $scope.messages = [];

  var ws = new WebSocket("ws://127.0.0.1:8080/chat/socket");

  ws.onopen = function() {
    console.log("connection established ...");
  }

  ws.onmessage = function(event) {
    var data = JSON.parse(event.data);
    var username = data['username'];
    var message = data['contents']['message'];

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
      contents: {
        chat_room: "lobby",
        message: $scope.enteredMessage,
      }
    }

    ws.send(JSON.stringify(message));
    $scope.enteredMessage = "";
  }
});
