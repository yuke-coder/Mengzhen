(function () {
    if (!window['__reloadWebviewInsertCode_has_init__']) {
        window['__reloadWebviewInsertCode_has_init__'] = true;

        var checkWebContainShowStatusCallbackId = 0;

        function nativeInsertCodeLog(msg) {
            var logMessage = 'nativeInsertCodeLog: ';
            if (typeof msg === 'object') {
                logMessage += JSON.stringify(msg);
            } else {
                logMessage += msg;
            }
            console && console.log && console.log(logMessage);
        }

        function dispatchNativeEventToDocument(eventName, data) {
            var event = new Event(eventName);
            event.data = data;
            document.dispatchEvent(event);
        }

        function reloadWebview() {
            try {
                if (window && window['_kg_account_has_changed_'] && !window['_prevent_webcontain_auto_reload_when_account_change_'] && location && typeof location.reload === 'function') {
                    location.reload();
                }
            } catch (e) {
                nativeInsertCodeLog(e);
            }
        }


        function checkWebContainShowStatusCallback(data) {
            try {
                nativeInsertCodeLog('callback: checkWebContainShowStatus, data:');
                nativeInsertCodeLog(data);
                if (data && data.code === 0 && data.data && data.data.isFront === 1) {
                    // webview容器显示
                    reloadWebview();
                }
            } catch (e) {
                nativeInsertCodeLog(e);
            }
        }

        function checkWebContainShowStatus() {
            var callbackKey = '__getWebContainShowStatus__callback__' + checkWebContainShowStatusCallbackId++;

            var scheme = 'jsbridge://karawebview/getWebContainShowStatus?p=' + encodeURIComponent(JSON.stringify({
                callback: callbackKey
            }));

            window[callbackKey] = checkWebContainShowStatusCallback;
            var iframe = document.createElement('iframe');
            iframe.style.cssText = 'display:none;width:0px;height:0px;';
            iframe.src = scheme;
            const container = document.body || document.documentElement;
            container.appendChild(iframe);
            nativeInsertCodeLog('call: checkWebContainShowStatus, scheme:' + scheme);
            setTimeout(function () {
                document.body.removeChild(iframe);
                iframe = null;
            }, 0);
        }

        function handleKgAccountChange() {
            /**
             * 客户端的账号变化事件通知最后会执行到全局的 kgAccountLoginStateChangeEvent 函数，并创建原生的 kgAccountLoginStateChangeEvent 事件dispatch到document
             * 如果页面有自己注册调用 KtvRoomInQmusicModule.kgAccountLoginStateChangeEvent ，则此处会被覆盖，页面将丢失账号变化后的自动刷新能力
             * */
            window['kgAccountLoginStateChangeEvent'] = function (data) {
                try {
                    nativeInsertCodeLog('event: kgAccountLoginStateChangeEvent, data:');
                    nativeInsertCodeLog(data);
                    /**设置账号变化标识，用于判断是否需要重新加载页面*/
                    window['_kg_account_has_changed_'] = true;

                    /**查询当前页面是否在前端显示，如果在前端显示，主动触发一次刷新检测*/
                    checkWebContainShowStatus();

                    dispatchNativeEventToDocument('kgAccountLoginStateChangeEvent', data);
                } catch (e) {
                    nativeInsertCodeLog(e);
                }
            }
        }

        function handleWebviewVisibilityChange() {
            /**
             * 当前 webview容器恢复显示通知最后会执行到全局的 WebContainShowEvent 函数，并创建原生的 WebContainShowEvent 事件dispatch到document
             * 里面会触发执行重新加载页面(重新加载的条件：账号登录态变化 window['_kg_account_has_changed_'] && 页面没有阻止刷新window['_prevent_webcontain_auto_reload_when_account_change_'])
             * 如果页面有自己注册调用 WebContain.WebContainShowEvent ，则此处会被覆盖，页面将丢失账号变化后的自动刷新能力
             * */
            window['WebContainShowEvent'] = function (data) {
                try {
                    nativeInsertCodeLog('event: WebContainShowEvent, data:');
                    nativeInsertCodeLog(data);
                    // 触发执行重新加载页面
                    reloadWebview();
                    dispatchNativeEventToDocument('WebContainShowEvent', data);
                } catch (e) {
                    nativeInsertCodeLog(e);
                }
            }

            /**
             * 当前 webview容器隐藏通知最后会执行到全局的 WebContainHideEvent 函数，并创建原生的 WebContainHideEvent 事件dispatch到document
             * 如果页面有自己注册调用 WebContain.WebContainHideEvent ，则此处会被覆盖
             * */
            window['WebContainHideEvent'] = function (data) {
                try {
                    nativeInsertCodeLog('event: WebContainHideEvent, data:');
                    nativeInsertCodeLog(data);
                    dispatchNativeEventToDocument('WebContainHideEvent', data);
                } catch (e) {
                    nativeInsertCodeLog(e);
                }
            }

        }

        function initEvent() {
            handleKgAccountChange();
            handleWebviewVisibilityChange();
        }

        initEvent();
    }
}());
