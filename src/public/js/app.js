var myModule = angular.module('myModule', []);

myModule.factory('MyService', ['$rootScope', function($rootScope) {
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

  Service.send = function(msg) {
    ws.send(msg);
  }

  return Service;
}]);

myModule.controller('MyCtrl', function($scope, MyService) {
  $scope.messages = MyService.messages;

  $scope.sendMessage = function() {
    MyService.send($scope.enteredName + ": " + $scope.enteredMessage);
    $scope.enteredMessage = "";
  }
});
