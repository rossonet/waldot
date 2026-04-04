#!/usr/bin/env python3
"""
OPC UA Client for WaldOT Industrial Monitoring Example
Tests connection and verifies node values
"""

import sys
import time
from asyncua import Client
import asyncio

OPCUA_ENDPOINT = "opc.tcp://waldot:12686/waldot"
TIMEOUT = 60  # seconds to wait for WaldOT to be ready

# Expected nodes to verify
EXPECTED_NODES = [
    "temp-office",
    "pressure-office",
    "temp-warehouse",
    "pressure-warehouse",
    "temp-production",
    "pressure-production",
]


async def test_waldot_connection():
    """Test WaldOT OPC UA server connection and node values"""

    print("=" * 70)
    print("WaldOT Industrial Monitoring - OPC UA Client Test")
    print("=" * 70)
    print()

    # Wait for WaldOT to be ready
    print(f"Waiting for WaldOT server at {OPCUA_ENDPOINT}...")
    start_time = time.time()

    while time.time() - start_time < TIMEOUT:
        try:
            async with Client(url=OPCUA_ENDPOINT) as client:
                print("✓ Connection successful!")
                break
        except Exception as e:
            print(f"  Waiting... ({int(time.time() - start_time)}s)")
            await asyncio.sleep(2)
    else:
        print(f"✗ FAILED: Could not connect to WaldOT after {TIMEOUT}s")
        return False

    print()

    # Connect and test
    try:
        async with Client(url=OPCUA_ENDPOINT) as client:
            print("Connected to WaldOT OPC UA Server")
            print()

            # Get root node
            root = client.get_root_node()
            print(f"Root node: {await root.read_browse_name()}")

            # Browse to Objects
            objects = await root.get_child(["0:Objects"])
            print(f"Objects node: {await objects.read_browse_name()}")

            # Browse to Gremlin Engine
            try:
                gremlin_engine = await objects.get_child(["2:Gremlin Engine"])
                print(f"Gremlin Engine node: {await gremlin_engine.read_browse_name()}")
            except Exception as e:
                print(
                    f"Warning: Could not find 'Gremlin Engine', trying alternatives..."
                )
                # Try to list children
                children = await objects.get_children()
                for child in children:
                    name = await child.read_browse_name()
                    print(f"  Found child: {name}")
                    if "gremlin" in str(name).lower() or "waldot" in str(name).lower():
                        gremlin_engine = child
                        break
                else:
                    gremlin_engine = objects

            print()
            print("-" * 70)
            print("Verifying sensor nodes...")
            print("-" * 70)

            # Get all children recursively
            all_nodes = {}

            async def browse_recursive(node, depth=0, max_depth=5):
                """Recursively browse nodes"""
                if depth > max_depth:
                    return

                try:
                    children = await node.get_children()
                    for child in children:
                        try:
                            browse_name = await child.read_browse_name()
                            name_str = str(browse_name.Name)
                            all_nodes[name_str] = child

                            # Recurse
                            await browse_recursive(child, depth + 1, max_depth)
                        except:
                            pass
                except:
                    pass

            await browse_recursive(gremlin_engine)

            print(f"\nFound {len(all_nodes)} nodes in total")
            print()

            # Verify expected sensors
            found_sensors = 0
            missing_sensors = []

            for expected in EXPECTED_NODES:
                if expected in all_nodes:
                    node = all_nodes[expected]
                    try:
                        # Try to read 'data' property
                        data_node = await node.get_child(["2:data"])
                        value = await data_node.read_value()
                        print(f"✓ {expected:25s} = {value:.2f}")
                        found_sensors += 1
                    except Exception as e:
                        print(f"⚠ {expected:25s} (found but no data property)")
                        found_sensors += 1
                else:
                    missing_sensors.append(expected)
                    print(f"✗ {expected:25s} NOT FOUND")

            print()
            print("-" * 70)
            print("Test Summary")
            print("-" * 70)
            print(f"Expected sensors: {len(EXPECTED_NODES)}")
            print(f"Found sensors:    {found_sensors}")
            print(f"Missing sensors:  {len(missing_sensors)}")

            if missing_sensors:
                print(f"\nMissing: {', '.join(missing_sensors)}")

            print()

            # Success criteria: at least 80% of sensors found
            success_rate = (found_sensors / len(EXPECTED_NODES)) * 100

            if success_rate >= 80:
                print(f"✓ TEST PASSED ({success_rate:.0f}% sensors found)")
                return True
            else:
                print(f"✗ TEST FAILED ({success_rate:.0f}% sensors found, need >= 80%)")
                return False

    except Exception as e:
        print(f"✗ ERROR: {e}")
        import traceback

        traceback.print_exc()
        return False


async def main():
    """Main entry point"""
    success = await test_waldot_connection()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    asyncio.run(main())
