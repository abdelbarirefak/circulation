const http = require('http');

const ports = [8080, 8081, 8085, 4000];
const paths = ['/api/map', '/api/stream'];

async function probe(port, path) {
    return new Promise((resolve) => {
        const req = http.get({
            host: 'localhost',
            port: port,
            path: path,
            timeout: 2000
        }, (res) => {
            let data = '';
            res.on('data', (chunk) => { data += chunk; if (data.length > 500) req.destroy(); });
            res.on('end', () => {
                console.log(`PORT ${port}${path}: STATUS ${res.statusCode}, BODY SAMPLE: ${data.substring(0, 100)}`);
                resolve();
            });
        });
        req.on('error', (err) => {
            console.log(`PORT ${port}${path}: FAILED (${err.message})`);
            resolve();
        });
        req.on('timeout', () => {
            console.log(`PORT ${port}${path}: TIMEOUT`);
            req.destroy();
            resolve();
        });
    });
}

(async () => {
    for (const port of ports) {
        for (const path of paths) {
            await probe(port, path);
        }
    }
})();
