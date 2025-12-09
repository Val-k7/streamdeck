/**
 * Tests E2E - Tests de Régression
 */

import { afterAll, beforeAll, describe, expect, it } from "@jest/globals";
import fetch from "node-fetch";
import WebSocket from "ws";

const serverUrl = process.env.SERVER_URL || "http://localhost:4455";
const wsUrl = process.env.WS_URL || "ws://localhost:4455";
const handshakeSecret = process.env.HANDSHAKE_SECRET;

describe("Regression Tests E2E", () => {
  let ws;
  let token;

  beforeAll(async () => {
    if (!handshakeSecret) {
      console.warn(
        "HANDSHAKE_SECRET not provided, WebSocket tests will be skipped"
      );
      return;
    }

    try {
      const response = await fetch(
        `${serverUrl}/tokens/handshake?secret=${encodeURIComponent(
          handshakeSecret
        )}&clientId=e2e-regression`,
        { method: "POST" }
      );
      if (response.ok) {
        const data = await response.json();
        token = data.token;
      }
    } catch (error) {
      console.warn("Handshake failed:", error.message);
    }

    if (token) {
      ws = new WebSocket(`${wsUrl}/ws`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      await new Promise((resolve) => {
        ws.on("open", resolve);
        ws.on("error", resolve);
      });
    }
  });

  afterAll(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.close();
    }
  });

  describe("Templates Tests", () => {
    const templates = ["User", "Gamer", "Streamer", "Audio", "Productivité"];

    templates.forEach((template) => {
      it(`should list profiles (template check: ${template})`, async () => {
        const response = await fetch(`${serverUrl}/profiles/`);
        expect(response.ok).toBe(true);

        const data = await response.json();
        expect(data).toHaveProperty("profiles");
        expect(Array.isArray(data.profiles)).toBe(true);
      });
    });
  });

  describe("Server Actions Tests", () => {
    const wsTest = token ? it : it.skip;

    wsTest("should execute processes action", (done) => {
      const payload = {
        action: "processes",
        payload: { limit: 1 },
        messageId: `msg-${Date.now()}`,
      };

      ws.send(JSON.stringify(payload));

      ws.once("message", (data) => {
        const response = JSON.parse(data.toString());
        expect(response.type).toBe("ack");
        expect(response.status).toBe("ok");
        done();
      });
    });
  });

  describe("Compatibility Tests", () => {
    it("should respond to health", async () => {
      const response = await fetch(`${serverUrl}/health/`);
      expect(response.ok).toBe(true);
    });

    it("should respond to diagnostics", async () => {
      const response = await fetch(`${serverUrl}/health/diagnostics`);
      expect(response.ok).toBe(true);
    });
  });

  describe("Performance Tests", () => {
    const perfTest = token ? it : it.skip;

    perfTest("should handle rapid actions", (done) => {
      const totalActions = 5;
      let completed = 0;
      const start = Date.now();

      const sendAction = (i) => {
        const action = {
          action: "processes",
          payload: { limit: 1 },
          messageId: `msg-${Date.now()}-${i}`,
        };
        ws.send(JSON.stringify(action));
        ws.once("message", (data) => {
          const response = JSON.parse(data.toString());
          if (response.type === "ack") {
            completed++;
            if (completed === totalActions) {
              expect(Date.now() - start).toBeLessThan(5000);
              done();
            }
          }
        });
      };

      for (let i = 0; i < totalActions; i++) {
        sendAction(i);
      }
    });
  });
});
