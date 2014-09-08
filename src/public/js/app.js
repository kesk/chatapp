var myModule = angular.module('myModule', ['ui.bootstrap']);

myModule.factory('SocketService', function($rootScope) {
  var Service = {};
  Service.messages = [];

  var ws = new WebSocket("ws://127.0.0.1:8080/com-channel");

  ws.onopen = function() {
    console.log("connection established ...");
  }

  ws.onmessage = function(event) {
    $rootScope.$apply(function() {
      Service.messages.push(event.data);
    });
  }

  ws.onclose = function(event) {
    console.log("connection closed");
  }

  Service.send = function(msg) {
    ws.send(msg);
  }

  return Service;
});

myModule.controller('MyCtrl', function($scope, SocketService) {
  $scope.messages = SocketService.messages;

  $scope.sendMessage = function() {
    SocketService.send($scope.enteredName + ": " + $scope.enteredMessage);
    $scope.enteredMessage = "";
  }
});
