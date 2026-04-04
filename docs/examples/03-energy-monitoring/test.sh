#!/bin/bash
set -e

echo "=========================================================================="
echo "WaldOT Example 03 - Energy Monitoring - Integration Test"
echo "=========================================================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Cleanup function
cleanup() {
    echo ""
    echo "Cleaning up..."
    docker-compose down -v 2>/dev/null || true
    echo "Cleanup complete"
}

# Set trap to cleanup on exit
trap cleanup EXIT

# Step 1: Clean start
echo "Step 1: Cleaning previous containers..."
docker-compose down -v 2>/dev/null || true
echo "✓ Cleanup complete"
echo ""

# Step 2: Build and start services
echo "Step 2: Building and starting services..."
docker-compose up -d --build
echo "✓ Services started"
echo ""

# Step 3: Wait for WaldOT to initialize
echo "Step 3: Waiting for WaldOT to initialize (70 seconds)..."
sleep 70
echo "✓ WaldOT initialization time elapsed"
echo ""

# Step 4: Check WaldOT logs for configuration success
echo "Step 4: Checking WaldOT configuration..."
sleep 10  # Give time for bootstrap to complete

WALDOT_LOGS=$(docker-compose logs waldot)

# Check for configuration complete message
if echo "$WALDOT_LOGS" | grep -q "Configuration Complete"; then
    echo "✓ Bootstrap configuration completed successfully"
else
    echo -e "${YELLOW}⚠ Warning: 'Configuration Complete' message not found in logs${NC}"
fi

# Check for expected sensors
EXPECTED_SENSORS=("power-office" "voltage-office" "current-office" "power-factory" "voltage-factory" "current-factory" "power-warehouse" "voltage-warehouse" "current-warehouse")
SENSORS_FOUND=0
for sensor in "${EXPECTED_SENSORS[@]}"; do
    if echo "$WALDOT_LOGS" | grep -qi "$sensor"; then
        echo "✓ Sensor '$sensor' configured"
        SENSORS_FOUND=$((SENSORS_FOUND + 1))
    else
        echo -e "${YELLOW}⚠ Warning: Sensor '$sensor' not found in logs${NC}"
    fi
done
echo "✓ Found $SENSORS_FOUND/${#EXPECTED_SENSORS[@]} sensors in logs"
echo ""

# Step 5: Run OPC UA client test
echo "Step 5: Running OPC UA client test..."
echo "----------------------------------------------------------------------"

# Run client and capture exit code
if docker-compose up opcua-client; then
    echo "----------------------------------------------------------------------"
    echo -e "${GREEN}✓ OPC UA client test PASSED${NC}"
    CLIENT_SUCCESS=true
else
    echo "----------------------------------------------------------------------"
    echo -e "${RED}✗ OPC UA client test FAILED${NC}"
    CLIENT_SUCCESS=false
fi
echo ""

# Step 6: Show logs summary
echo "Step 6: Logs Summary"
echo "=========================================================================="
echo ""
echo "WaldOT Logs (last 30 lines):"
echo "--------------------------------------------------------------------------"
docker-compose logs --tail=30 waldot
echo ""
echo "OPC UA Client Logs:"
echo "--------------------------------------------------------------------------"
docker-compose logs opcua-client
echo ""

# Step 7: Final result
echo "=========================================================================="
echo "Test Results"
echo "=========================================================================="

if [ "$CLIENT_SUCCESS" = true ]; then
    echo -e "${GREEN}✓ ALL TESTS PASSED${NC}"
    echo ""
    echo "The Energy Monitoring example is working correctly!"
    echo "You can now connect with an OPC UA client to: opc.tcp://localhost:12688/waldot"
    exit 0
else
    echo -e "${RED}✗ TESTS FAILED${NC}"
    echo ""
    echo "Please check the logs above for errors."
    exit 1
fi
