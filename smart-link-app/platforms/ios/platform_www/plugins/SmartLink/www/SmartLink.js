cordova.define("SmartLink.SmartLink", function(require, exports, module) {
var exec = require('cordova/exec');

exports.coolMethod = function(arg0, success, error) {
    exec(success, error, "SmartLink", "coolMethod", [arg0]);
};

});
