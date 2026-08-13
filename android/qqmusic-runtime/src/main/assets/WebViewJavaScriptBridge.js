;(function() 
{
	if (window.WebViewJavascriptBridge) 
	{ return }
	
	
	var responseCallbacks = {}
	var uniqueId = 1
	
    function init() 
	{
		init(function(message, responseCallback) {});
	}

	function init(messageHandler) 
	{
		if (WebViewJavascriptBridge._messageHandler) 
		{ throw new Error('WebViewJavascriptBridge.init called twice') }
		WebViewJavascriptBridge._messageHandler = messageHandler
	}
		
	function callHandler(handlerName, data, responseCallback)
	{
		_doSendToJava(handlerName, data,responseCallback)
	}
	
	function _doSendToJava(handlerName,data, responseCallback) 
	{
		if (responseCallback) 
		{
			var callbackId = 'cb_'+(uniqueId++)+'_'+new Date().getTime()
			responseCallbacks[callbackId] = responseCallback
		}
		data = typeof(data) == "string"? ([data]) : data;
		window.Bridge.sendJavaScriptRequest(handlerName,JSON.stringify(data),'window.WebViewJavascriptBridge.callbackToBridge',callbackId);
	}
	
	function callbackToBridge(respose)
	{
		_handleMessageFromJava(respose)
	}

	function _dispatchMessageFromJava(messageJSON) 
	{
		setTimeout(function _timeoutDispatchMessageFromObjC() 
		{
			var message = JSON.parse(messageJSON), flag = 0, key,rs = message.response;
			
			if (typeof(rs.response) === "object") {
				for (var k in rs.response) {
					key = k;
					flag ++;
				}
				
				if (flag == 1) {
					rs = message.response[key];
				}
			}
			
			if (message.responseId)
			{
				var responseCallback = responseCallbacks[message.responseId]
				if (!responseCallback) 
				{ return; }
				responseCallback(rs);
				delete responseCallbacks[message.responseId]
			}
		})
	}
	
	function _handleMessageFromJava(messageJSON) 
	{
		_dispatchMessageFromJava(messageJSON)
	}

	window.WebViewJavascriptBridge = 
	{
		init: init,
		callbackToBridge: callbackToBridge,
		callHandler: callHandler,
		_handleMessageFromJava: _handleMessageFromJava
	}

	//var doc = document
	var readyEvent = document.createEvent('Events');
	readyEvent.initEvent('WebViewJavascriptBridgeReady', true, true);
	readyEvent.bridge = WebViewJavascriptBridge;
	
	document.dispatchEvent(readyEvent);

})(); 




