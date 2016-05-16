var exec = require('cordova/exec');

module.exports = {
  NELPBufferStrategy: {
    NELPLowDelay: 0,
    NELPAntiJitter: 1
  },

  NELPMovieScalingMode: {
    NELPMovieScalingModeNone: 0,
    NELPMovieScalingModeAspectFit: 1,
    NELPMovieScalingModeAspectFill: 2,
    NELPMovieScalingModeFill: 3
  },

  options: {
    bufferStrategy: 0, // NELPLowDelay || NELPAntiJitter
    scalingMode: 0, // NELPMovieScalingModeNone || NELPMovieScalingModeAspectFit || NELPMovieScalingModeAspectFill || NELPMovieScalingModeFill
    shouldAutoplay: true, // true || false
    pauseInBackground: false, // true || false
    hardwareDecoder: false // true || false
  },

  play: function (url, title, options, successCallback, errorCallback) {
    // options turned off.
    options = options || {};
    options.bufferStrategy = options.bufferStrategy || 0;
    options.scalingMode = options.scalingMode || 0;
    options.shouldAutoplay = typeof options.shouldAutoplay !== 'undefined' ? options.shouldAutoplay : true;
    options.pauseInBackground = typeof options.pauseInBackground !== 'undefined' ? options.pauseInBackground : true;
    options.hardwareDecoder = typeof options.hardwareDecoder !== 'undefined' ? options.hardwareDecoder : false;

    exec(successCallback, errorCallback, "LivePlayer", "play", [url, title, options]);
  }
};