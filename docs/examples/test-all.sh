#!/bin/bash
set -e

echo "=========================================================================="
echo "WaldOT Examples - Complete Test Suite"
echo "=========================================================================="
echo ""
echo "This script will test all 5 WaldOT examples sequentially."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Track results
TOTAL_TESTS=5
PASSED_TESTS=0
FAILED_TESTS=0

# Test results array
declare -a TEST_RESULTS

# Function to run a single example test
run_example_test() {
    local example_num=$1
    local example_name=$2
    local example_dir=$3
    
    echo ""
    echo -e "${BLUE}=========================================================================="
    echo "Testing Example $example_num: $example_name"
    echo -e "==========================================================================${NC}"
    echo ""
    
    cd "$example_dir"
    
    if ./test.sh; then
        echo -e "${GREEN}✓ Example $example_num PASSED${NC}"
        TEST_RESULTS[$example_num]="PASSED"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}✗ Example $example_num FAILED${NC}"
        TEST_RESULTS[$example_num]="FAILED"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Run all tests
START_TIME=$(date +%s)

run_example_test 1 "Industrial Monitoring" "/work/waldot/docs/examples/01-industrial-monitoring"
run_example_test 2 "Production Simulation" "/work/waldot/docs/examples/02-production-simulation"
run_example_test 3 "Energy Monitoring" "/work/waldot/docs/examples/03-energy-monitoring"
run_example_test 4 "Quality Control" "/work/waldot/docs/examples/04-quality-control"
run_example_test 5 "Predictive Maintenance" "/work/waldot/docs/examples/05-predictive-maintenance"

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# Print summary
echo ""
echo "=========================================================================="
echo "Test Suite Summary"
echo "=========================================================================="
echo ""
echo "Total Tests:   $TOTAL_TESTS"
echo -e "Passed:        ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed:        ${RED}$FAILED_TESTS${NC}"
echo "Duration:      ${DURATION}s"
echo ""

# Print individual results
echo "Individual Results:"
echo "-------------------"
for i in {1..5}; do
    result="${TEST_RESULTS[$i]}"
    if [ "$result" = "PASSED" ]; then
        echo -e "Example $i: ${GREEN}✓ PASSED${NC}"
    else
        echo -e "Example $i: ${RED}✗ FAILED${NC}"
    fi
done
echo ""

# Final verdict
if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}=========================================================================="
    echo "✓ ALL TESTS PASSED - 100% SUCCESS RATE"
    echo -e "==========================================================================${NC}"
    echo ""
    echo "All WaldOT examples are working correctly!"
    echo ""
    echo "You can now run individual examples with:"
    echo "  cd /work/waldot/docs/examples/0X-example-name"
    echo "  docker-compose up -d"
    echo ""
    exit 0
else
    echo -e "${RED}=========================================================================="
    echo "✗ SOME TESTS FAILED"
    echo -e "==========================================================================${NC}"
    echo ""
    echo "Please check the logs above for errors."
    echo ""
    exit 1
fi
