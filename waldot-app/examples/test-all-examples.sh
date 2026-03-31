#!/bin/bash

# Script to test all Docker Compose examples
# This script starts each example, waits for health check, and then stops it

set -e

# Colors for output
RED='\033[0:31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Timeout for health checks (seconds)
TIMEOUT=120

# Function to print colored messages
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Function to wait for service to be healthy
wait_for_healthy() {
    local compose_file=$1
    local service_name=${2:-waldot}
    local max_wait=$TIMEOUT
    local elapsed=0
    
    print_info "Waiting for $service_name to be healthy (max ${max_wait}s)..."
    
    while [ $elapsed -lt $max_wait ]; do
        health_status=$(docker-compose -f "$compose_file" ps -q "$service_name" 2>/dev/null | xargs docker inspect --format='{{.State.Health.Status}}' 2>/dev/null || echo "starting")
        
        if [ "$health_status" = "healthy" ]; then
            print_info "$service_name is healthy after ${elapsed}s"
            return 0
        fi
        
        if [ "$health_status" = "unhealthy" ]; then
            print_error "$service_name is unhealthy"
            docker-compose -f "$compose_file" logs "$service_name"
            return 1
        fi
        
        sleep 2
        elapsed=$((elapsed + 2))
        echo -n "."
    done
    
    echo ""
    print_error "Timeout waiting for $service_name to be healthy"
    docker-compose -f "$compose_file" logs "$service_name"
    return 1
}

# Function to test a single docker-compose file
test_example() {
    local compose_file=$1
    local example_name=$(basename "$compose_file" .yml)
    
    print_info "========================================="
    print_info "Testing: $example_name"
    print_info "========================================="
    
    # Clean up any existing containers
    print_info "Cleaning up existing containers..."
    docker-compose -f "$compose_file" down -v 2>/dev/null || true
    
    # Start the services
    print_info "Starting services..."
    if ! docker-compose -f "$compose_file" up -d; then
        print_error "Failed to start services"
        return 1
    fi
    
    # Get list of services
    services=$(docker-compose -f "$compose_file" config --services)
    
    # Wait for each service to be healthy
    for service in $services; do
        if ! wait_for_healthy "$compose_file" "$service"; then
            print_error "Health check failed for $service"
            docker-compose -f "$compose_file" down -v
            return 1
        fi
    done
    
    # Show logs
    print_info "Service logs:"
    docker-compose -f "$compose_file" logs --tail=20
    
    # Show running containers
    print_info "Running containers:"
    docker-compose -f "$compose_file" ps
    
    # Clean up
    print_info "Cleaning up..."
    docker-compose -f "$compose_file" down -v
    
    print_info "✓ $example_name test passed"
    echo ""
    
    return 0
}

# Main test execution
main() {
    local script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    cd "$script_dir"
    
    print_info "Starting WaldOT Docker Compose examples testing"
    print_info "Script directory: $script_dir"
    echo ""
    
    # Find all docker-compose files
    compose_files=(docker-compose-*.yml)
    
    if [ ${#compose_files[@]} -eq 0 ]; then
        print_error "No docker-compose files found"
        exit 1
    fi
    
    print_info "Found ${#compose_files[@]} docker-compose files to test"
    echo ""
    
    # Track results
    total=0
    passed=0
    failed=0
    failed_examples=()
    
    # Test each example
    for compose_file in "${compose_files[@]}"; do
        total=$((total + 1))
        
        if test_example "$compose_file"; then
            passed=$((passed + 1))
        else
            failed=$((failed + 1))
            failed_examples+=("$compose_file")
        fi
        
        # Small delay between tests
        sleep 2
    done
    
    # Print summary
    echo ""
    print_info "========================================="
    print_info "TEST SUMMARY"
    print_info "========================================="
    print_info "Total examples: $total"
    print_info "Passed: $passed"
    
    if [ $failed -gt 0 ]; then
        print_error "Failed: $failed"
        print_error "Failed examples:"
        for example in "${failed_examples[@]}"; do
            print_error "  - $example"
        done
        exit 1
    else
        print_info "All tests passed!"
        exit 0
    fi
}

# Run main function
main
