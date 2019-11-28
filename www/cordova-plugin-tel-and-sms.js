var exec = require('cordova/exec');
exports.callTel = function(arg0, success, error) {
  exec(success, error, 'callTELAndSMSPlugin', 'callTel', [arg0]);
};
exports.callSMS = function(arg0, success, error) {
  exec(success, error, 'callTELAndSMSPlugin', 'callSMS', [arg0]);
};
