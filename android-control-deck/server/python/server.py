import asyncio
import json
import websockets
from pathlib import Path

MAPPINGS_PATH = Path(__file__).parent.parent / "config" / "mappings.json"
TOKEN = "change-me"

async def load_mappings():
    if MAPPINGS_PATH.exists():
        return json.loads(MAPPINGS_PATH.read_text())
    return {}

async def handler(websocket):
    mappings = await load_mappings()
    async for message in websocket:
        try:
            payload = json.loads(message)
            mapping = mappings.get(payload.get("controlId"))
            if not mapping:
                continue
            print("[python action]", mapping)
            # TODO: implement keyboard/obs/audio integrations
        except Exception as exc:
            print("Invalid message", exc)

async def main():
    async with websockets.serve(handler, "0.0.0.0", 4456):
        print("Python server listening on ws://0.0.0.0:4456/ws")
        await asyncio.Future()

if __name__ == "__main__":
    asyncio.run(main())
