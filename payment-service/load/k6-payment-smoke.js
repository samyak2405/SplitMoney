import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 25,
  duration: "1m",
};

export default function () {
  const idempotencyKey = `${__VU}-${__ITER}-${Date.now()}`;
  const payload = JSON.stringify({
    payerUserId: "00000000-0000-0000-0000-000000000001",
    payeeUserId: "00000000-0000-0000-0000-000000000002",
    amount: 100.0,
    currency: "INR",
    paymentMethod: "UPI",
    clientRequestId: `req-${idempotencyKey}`,
  });

  const response = http.post("http://localhost:8080/v1/payments", payload, {
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": idempotencyKey,
    },
  });

  check(response, {
    "status is 201/409": (r) => r.status === 201 || r.status === 409,
  });
  sleep(0.1);
}
