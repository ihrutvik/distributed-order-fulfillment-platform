import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const replayMismatches = new Counter('idempotency_replay_mismatches');
const successfulOrders = new Rate('successful_order_requests');
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const apiKey = __ENV.API_KEY || 'demo-secret';
const headers = {'Content-Type': 'application/json', 'X-Api-Key': apiKey};

export const options = {
  scenarios: {
    sustained_orders: {
      executor: 'constant-arrival-rate',
      exec: 'createAndReplay',
      rate: 20,
      timeUnit: '1s',
      duration: '2m',
      preAllocatedVUs: 20,
      maxVUs: 80,
    },
    concurrent_same_key: {
      executor: 'per-vu-iterations',
      exec: 'raceSameIdempotencyKey',
      vus: 20,
      iterations: 5,
      startTime: '10s',
      maxDuration: '1m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    successful_order_requests: ['rate>0.99'],
    idempotency_replay_mismatches: ['count==0'],
  },
};

function postOrder(clientOrderId) {
  return http.post(`${baseUrl}/v1/orders`, JSON.stringify({
    clientOrderId,
    amount: 79.99,
    currency: 'EUR',
    sku: 'DEMO-SKU',
    quantity: 1,
  }), {headers, tags: {operation: 'create_order'}});
}

export function createAndReplay() {
  const key = `load-${__VU}-${__ITER}-${Date.now()}`;
  const first = postOrder(key);
  const replay = postOrder(key);
  const accepted = check(first, {'new order accepted': response => response.status === 201});
  const replayed = check(replay, {'idempotent replay accepted': response => response.status === 201});
  let sameOrder = false;
  if (accepted && replayed) {
    try { sameOrder = first.json('id') === replay.json('id'); } catch (_) { sameOrder = false; }
  }
  if (!sameOrder) replayMismatches.add(1);
  successfulOrders.add(accepted && replayed && sameOrder);
}

export function raceSameIdempotencyKey() {
  const response = postOrder(`race-${__ITER}`);
  const accepted = check(response, {'concurrent retry accepted': result => result.status === 201});
  successfulOrders.add(accepted);
}
