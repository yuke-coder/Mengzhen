(function () {
    if (window['__webview_hook_console_has_init__']) {
        return;
    }

    if (!window.console) {
        return;
    }
    window['__webview_hook_console_has_init__'] = true;

    var originalConsoleInfo = window.console.info;
    var originalConsoleWarn = window.console.warn;
    var originalConsoleError = window.console.error;

    function processArguments(level, args) {
        var message = '';
        try {
            for (var i = 0; i < args.length; i++) {
                if (typeof args[i] === 'object') {
                    message += JSON.stringify(args[i]);
                } else {
                    message += args[i];
                }
                message += ' ';
            }
        } catch (e) {
        }
        return '[' + level + '] ' + message;
    }

    window.console.info = function () {
        originalConsoleInfo.apply(window.console, arguments);
        originalConsoleInfo.apply(window.console, [processArguments('info', arguments)]);
    };

    window.console.warn = function () {
        originalConsoleWarn.apply(window.console, arguments);
        originalConsoleWarn.apply(window.console, [processArguments('warn', arguments)]);
    };

    window.console.error = function () {
        originalConsoleError.apply(window.console, arguments);
        originalConsoleError.apply(window.console, [processArguments('error', arguments)]);
    };

}());
