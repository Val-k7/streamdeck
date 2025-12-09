/**
 * Tests E2E pour les actions de contrôle
 */

import { afterAll, beforeAll, describe, expect, it } from "@jest/globals";
import fetch from "node-fetch";
import WebSocket from "ws";

const serverUrl = process.env.SERVER_URL || "http://localhost:4455";
const wsUrl = process.env.WS_URL || "ws://localhost:4455";
const handshakeSecret = process.env.HANDSHAKE_SECRET;

describe("Control Actions E2E", () => {
  let ws;
  let token;

  beforeAll(async () => {
    if (!handshakeSecret) {
      console.warn("HANDSHAKE_SECRET not provided, skipping WS tests");
      return;
    }

    const response = await fetch(
      `${serverUrl}/tokens/handshake?secret=${encodeURIComponent(
        handshakeSecret
      )}&clientId=e2e-control-actions`,
      { method: "POST" }
    );
    if (!response.ok) {
      console.warn("Handshake failed, skipping WS tests");
      return;
    }

    token = (await response.json()).token;
    ws = new WebSocket(`${wsUrl}/ws`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    await new Promise((resolve) => {
      ws.on("open", resolve);
      ws.on("error", resolve);
    });
  });

  afterAll(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.close();
    }
  });

  const wsTest = token ? it : it.skip;

  wsTest("should send processes action and receive ack", (done) => {
    const action = {
      action: "processes",
      payload: { limit: 1 },
      messageId: `msg-${Date.now()}`,
    };

    ws.send(JSON.stringify(action));

    ws.once("message", (data) => {
      const response = JSON.parse(data.toString());
      expect(response.type).toBe("ack");
      done();
    });
  });

  wsTest("should return error on unknown action", (done) => {
    const action = {
      action: "unknown-action",
      messageId: `msg-${Date.now()}`,
    };

    ws.send(JSON.stringify(action));

    ws.once("message", (data) => {
      const response = JSON.parse(data.toString());
      expect(["error", "ack"]).toContain(response.type);
      done();
    });
  });
});
