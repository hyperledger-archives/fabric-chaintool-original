var http = require('http');
var pb = require("protobufjs");

var builder = pb.loadProtoFile("./appinit.proto"),
    app = builder.build("appinit");

function Deploy(id, params) {

    var post_data = JSON.stringify({
        'jsonrpc': '2.0',
        'method': 'deploy',
        'params': {
            'type': 1,
            'chaincodeID': id,
            'ctorMsg': {
                'function':'init',
                'args':[new app.Init(params).toBase64()]
            }
        },
        "id": 1
    });
    console.log(post_data);
    post(post_data, '/chaincode',function(body) {
        var resp = JSON.parse(body);
        console.log(resp.result.message);
    });
}

function post(pdata, path) {
    var post_options = {
        host: 'localhost',
        port: '3000',
        path: path,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    };

    var post_req = http.request(post_options, function(res) {
        res.setEncoding('utf8');
        res.on('data', function (chunk) {
            console.log('Response: ' + chunk);
        });
    });

    // post the data
    post_req.write(pdata);
    post_req.end();

}

Deploy(
    {
        'name': 'mycc'
    },
    {
        'partyA': {
            'entity': 'foo',
            'value': 100
        },
        'partyB': {
            'entity': 'bar',
            'value': 100
        }
    }
);
