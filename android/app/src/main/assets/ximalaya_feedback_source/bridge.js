(function () {
    let pendingResolve = null;

    window.submitMengzhenFeedback = function (payload) {
        return new Promise(function (resolve) {
            pendingResolve = resolve;
            if (!window.MengzhenFeedback || !window.MengzhenFeedback.submit) {
                pendingResolve = null;
                resolve({ code: 500, message: '反馈服务不可用' });
                return;
            }
            window.MengzhenFeedback.submit(JSON.stringify(payload));
        });
    };

    window.completeMengzhenFeedback = function (success, message) {
        if (!pendingResolve) return;
        const resolve = pendingResolve;
        pendingResolve = null;
        resolve({ code: success ? 200 : 500, message: message || '' });
    };
})();
