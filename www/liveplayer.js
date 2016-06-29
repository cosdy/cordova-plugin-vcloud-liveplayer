var exec = require('cordova/exec');

module.exports = {
  play: function (url, title, successCallback, errorCallback) {
    exec(successCallback, errorCallback, "LivePlayer", "play", [url, title]);
  }
};