var exec = require('cordova/exec');

exports.coolMethod = function(arg0, success, error) {
    exec(success, error, "SmartLink", "coolMethod", [arg0]);
};

exports.getSSID = function(arg0, success, error) {
    exec(success, error, "SmartLink", "getSSID", [arg0]);
};

exports.connect = function(arg0, success, error) {
    exec(success, error, "SmartLink", "connect", [arg0]);
};