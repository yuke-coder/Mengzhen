function sonicCallNative(module, method, params, cid) {
    hippyCallNatives(module, method, cid, [params])
};

global.hippyBridge = (_action, _callObj) => {
    let resp = 'success';
    let action = _action;
    let callObj = _callObj;

    switch (action) {
        case "sonicCallJs": {
            sonicCallJs(callObj[0], callObj[1])
            break;
        }
        default: {
            resp = 'error: action not define';
            break;
        }
    }

    return resp;
};



