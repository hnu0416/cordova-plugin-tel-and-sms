## 电话&短信插件说明

#### 1、 将插件工程下载本地（可通过用户名密码形式直接从git拉取）

	cordova plugin add cordova-plugin-tel-and-sms（插件解压后的目录）
	
#### 2、该插件仅支持安卓4.4及以上&iOS8.0以上

#### 3、调用插件：

##### 3-1、发送信息

##### 方法
	window.callTelOrSMS.callSMS([phoneNumber], success, error);
	phoneNumber => 手机号
	success&fail => 回调函数
	
##### 实例
	function sendSMS () {
		 var options = {
		 	message: '您确定要继续该操作吗?',
		 	title: '操作警告',
		 	buttonLabels: ['取消', '发送'],
		 	animation: 'default',
		 	primaryButtonIndex: 1,
		 	cancelable: true,
		 	callback: function (index) {
		 		if (index === 1) {
		 			window.callTelOrSMS.callSMS(
		 			{
		 				phones: ["18616174832"],
		 				content: "hello world!"
		 			},
		 			function(){
		 				showAlert("send success.");
		 			},
		 			function(){
		 				showAlert("send fail.");
		 			});
		 		} else {
		 			showAlert("取消发送...");
		 		}
	   		}
	   	};
	   ons.notification.confirm(options);
	};
	
##### 3-2、调用电话  

##### 方法说明
	window.callTelOrSMS.callTel(phoneNumber, success, error)
	phoneNumber => 手机号
	success&fail => 回调函数
	备注：success会返回结束电话的时间，格式均为{ messsage: "" }
	
##### 实例
	function call() {
		window.callTelOrSMS.callTel(
			"18616174832", 
			function(data){
				showAlert("call success => " + data.message);
			},function(data){
				showAlert("call fail => " + data.message);
			}
		);
	}
	
	
