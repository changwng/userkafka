#!/bin/bash

# Kafka Replay Script for User Events
# This script helps replay user events from a specific offset

KAFKA_HOME=/opt/kafka
BOOTSTRAP_SERVERS=localhost:9092
TOPIC=user-events

# Function to display usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  -t, --topic TOPIC        Topic name (default: user-events)"
    echo "  -s, --servers SERVERS    Bootstrap servers (default: localhost:9092)"
    echo "  -o, --offset OFFSET      Starting offset (default: earliest)"
    echo "  -p, --partition PARTITION Partition number (default: all)"
    echo "  -h, --help               Show this help message"
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--topic)
            TOPIC="$2"
            shift 2
            ;;
        -s|--servers)
            BOOTSTRAP_SERVERS="$2"
            shift 2
            ;;
        -o|--offset)
            OFFSET="$2"
            shift 2
            ;;
        -p|--partition)
            PARTITION="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "Unknown option: $1"
            usage
            ;;
    esac
}

echo "Kafka Replay Script"
echo "==================="
echo "Topic: $TOPIC"
echo "Bootstrap Servers: $BOOTSTRAP_SERVERS"
echo "Starting Offset: ${OFFSET:-earliest}"
echo "Partition: ${PARTITION:-all}"
echo

# Check if topic exists
echo "Checking if topic exists..."
if ! kafka-topics --bootstrap-server $BOOTSTRAP_SERVERS --list | grep -q "^${TOPIC}$"; then
    echo "Error: Topic '$TOPIC' does not exist"
    exit 1
fi

# Show topic information
echo "Topic Information:"
kafka-topics --bootstrap-server $BOOTSTRAP_SERVERS --describe --topic $TOPIC

echo
echo "Current Consumer Groups:"
kafka-consumer-groups --bootstrap-server $BOOTSTRAP_SERVERS --list | grep -E "(post-service|user-view)"

echo
echo "Consumer Group Details:"
kafka-consumer-groups --bootstrap-server $BOOTSTRAP_SERVERS --describe --group post-service-streams

echo
echo "Starting replay..."
echo "Press Ctrl+C to stop"

# Build consumer command
CONSUMER_CMD="kafka-console-consumer --bootstrap-server $BOOTSTRAP_SERVERS --topic $TOPIC --property print.key=true --property key.separator=': '"

if [ -n "$OFFSET" ]; then
    CONSUMER_CMD="$CONSUMER_CMD --from-beginning"
fi

if [ -n "$PARTITION" ]; then
    CONSUMER_CMD="$CONSUMER_CMD --partition $PARTITION"
fi

# Execute consumer
eval $CONSUMER_CMD 