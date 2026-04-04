#!/usr/bin/env python3
"""OPC UA Client for WaldOT Quality Control Example"""

import sys, time, asyncio
from asyncua import Client

OPCUA_ENDPOINT = "opc.tcp://waldot:12686/waldot"  # Internal port is always 12686
TIMEOUT = 60
EXPECTED_NODES = [
    "quality-checkpoint-1",
    "quality-checkpoint-2",
    "quality-checkpoint-3",
    "quality-checkpoint-4",
    "defect-detector",
]


async def test_waldot_connection():
    print(
        "=" * 70 + "\nWaldOT Quality Control - OPC UA Client Test\n" + "=" * 70 + "\n"
    )
    print(f"Waiting for WaldOT server at {OPCUA_ENDPOINT}...")
    start_time = time.time()
    while time.time() - start_time < TIMEOUT:
        try:
            async with Client(url=OPCUA_ENDPOINT) as client:
                print("✓ Connection successful!\n")
                break
        except:
            print(f"  Waiting... ({int(time.time() - start_time)}s)")
            await asyncio.sleep(2)
    else:
        print(f"✗ FAILED: Could not connect after {TIMEOUT}s")
        return False

    try:
        async with Client(url=OPCUA_ENDPOINT) as client:
            print(
                "Connected to WaldOT OPC UA Server\n"
                + "-" * 70
                + "\nVerifying sensor nodes...\n"
                + "-" * 70
                + "\n"
            )
            root = client.get_root_node()
            objects = await root.get_child(["0:Objects"])
            try:
                gremlin_engine = await objects.get_child(["2:Gremlin Engine"])
            except:
                children = await objects.get_children()
                gremlin_engine = next(
                    (
                        c
                        for c in children
                        if "gremlin" in str(await c.read_browse_name()).lower()
                    ),
                    objects,
                )

            all_nodes = {}

            async def browse_recursive(node, depth=0, max_depth=5):
                if depth > max_depth:
                    return
                try:
                    for child in await node.get_children():
                        try:
                            name_str = str((await child.read_browse_name()).Name)
                            all_nodes[name_str] = child
                            await browse_recursive(child, depth + 1, max_depth)
                        except:
                            pass
                except:
                    pass

            await browse_recursive(gremlin_engine)
            print(f"Found {len(all_nodes)} nodes in total\n")

            found_sensors = sum(1 for n in EXPECTED_NODES if n in all_nodes)
            missing = [n for n in EXPECTED_NODES if n not in all_nodes]

            for expected in EXPECTED_NODES:
                status = "✓ FOUND" if expected in all_nodes else "✗ NOT FOUND"
                print(f"{status:10s} {expected}")

            print(f"\n{'-'*70}\nTest Summary\n{'-'*70}")
            print(
                f"Expected: {len(EXPECTED_NODES)}, Found: {found_sensors}, Missing: {len(missing)}"
            )
            if missing:
                print(f"Missing: {', '.join(missing)}")

            success_rate = (found_sensors / len(EXPECTED_NODES)) * 100
            result = success_rate >= 80
            print(
                f"\n{'✓ TEST PASSED' if result else '✗ TEST FAILED'} ({success_rate:.0f}% found)\n"
            )
            return result
    except Exception as e:
        print(f"✗ ERROR: {e}")
        import traceback

        traceback.print_exc()
        return False


if __name__ == "__main__":
    asyncio.run(test_waldot_connection()) and sys.exit(0) or sys.exit(1)
