var exec = require('cordova/exec');

module.exports = {
  play: function (url, title, successCallback, errorCallback) {
    exec(successCallback, errorCallback, "LivePlayer", "play", [url, title]);
  },

  channel: function (name, message, successCallback, errorCallback) {
    exec(successCallback, errorCallback, "LivePlayer", "channel", [name, message]);
  },

  message: function (successCallback, errorCallback) {
    exec(successCallback, errorCallback, "LivePlayer", "message", []);
  }
};