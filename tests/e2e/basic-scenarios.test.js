/**
 * Tests End-to-End - Scénarios de base
 *
 * Ces tests nécessitent un serveur en cours d'exécution et un client Android simulé
 */

import { beforeAll, describe, expect, it } from "@jest/globals";
import fetch from "node-fetch";
import { WebSocket } from "ws";

const SERVER_URL = process.env.SERVER_URL || "http://localhost:4455";
const WS_URL = process.env.WS_URL || "ws://localhost:4455";
const HANDSHAKE_SECRET = process.env.HANDSHAKE_SECRET;

describe("E2E - Scénarios Utilisateur", () => {
  let token = null;

  beforeAll(async () => {
    if (!HANDSHAKE_SECRET) {
      console.warn(
        "HANDSHAKE_SECRET not provided, skipping authenticated WS tests"
      );
      return;
    }

    try {
      const response = await fetch(
        `${SERVER_URL}/tokens/handshake?secret=${encodeURIComponent(
          HANDSHAKE_SECRET
        )}&clientId=e2e-test-client`,
        { method: "POST" }
      );
      if (response.ok) {
        const data = await response.json();
        token = data.token;
      } else {
        console.warn("Handshake failed with status", response.status);
      }
    } catch (e) {
      console.warn("Could not get token for E2E tests:", e.message);
    }
  });

  describe("Scénario 1: Premier Lancement", () => {
    it("should discover server", async () => {
      const response = await fetch(`${SERVER_URL}/discovery/`);
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(data).toHaveProperty("serverId");
      expect(data).toHaveProperty("serverName");
    });

    it("should request pairing code", async () => {
      const response = await fetch(`${SERVER_URL}/discovery/pairing/request`, {
        method: "POST",
      });

      expect(response.ok).toBe(true);
      const data = await response.json();
      expect(data).toHaveProperty("code");
      expect(data.code).toMatch(/^[a-f0-9]{8}$/);
    });

    it("should complete pairing and connect", async () => {
      // Request pairing
      const requestResponse = await fetch(
        `${SERVER_URL}/discovery/pairing/request`,
        {
          method: "POST",
        }
      );
      const requestData = await requestResponse.json();
      const code = requestData.code;
      const serverId = requestData.serverId;

      // Confirm pairing
      const confirmResponse = await fetch(
        `${SERVER_URL}/discovery/pairing/confirm?code=${code}&serverId=${serverId}&fingerprint=e2e-fingerprint`,
        { method: "POST" }
      );

      expect(confirmResponse.ok).toBe(true);
      const confirmData = await confirmResponse.json();
      expect(confirmData).toHaveProperty("status", "paired");
      expect(confirmData).toHaveProperty("serverId", serverId);
    });
  });

  describe("Scénario 2: Utilisation Basique", () => {
    it("should list available profiles", async () => {
      const response = await fetch(`${SERVER_URL}/profiles/`);
      expect(response.ok).toBe(true);

      const data = await response.json();
      expect(data).toHaveProperty("profiles");
      expect(Array.isArray(data.profiles)).toBe(true);
    });

    const wsTest = token ? it : it.skip;

    wsTest("should execute a simple action over WebSocket", (done) => {
      if (!token) return done();

      const ws = new WebSocket(`${WS_URL}/ws`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });

      ws.on("open", () => {
        const payload = {
          action: "processes",
          payload: { limit: 1 },
          messageId: `e2e-${Date.now()}`,
        };

        ws.send(JSON.stringify(payload));

        ws.on("message", (data) => {
          const response = JSON.parse(data.toString());
          if (response.type === "ack") {
            expect(response.status).toBe("ok");
            ws.close();
            done();
          }
        });
      });

      ws.on("error", (error) => {
        done(error);
      });
    });
  });

  describe("Scénario 3: Gestion d'Erreurs", () => {
    const wsErrorTest = token ? it : it.skip;

    wsErrorTest("should handle invalid action gracefully", (done) => {
      if (!token) return done();

      const ws = new WebSocket(`${WS_URL}/ws`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      ws.on("open", () => {
        const payload = {
          action: "unknown-action",
          payload: {},
          messageId: `e2e-${Date.now()}`,
        };

        ws.send(JSON.stringify(payload));

        ws.on("message", (data) => {
          const response = JSON.parse(data.toString());
          if (response.type === "ack") {
            expect(response.status).toBe("error");
            ws.close();
            done();
          }
        });
      });

      ws.on("error", (error) => {
        done(error);
      });
    });
  });
});
