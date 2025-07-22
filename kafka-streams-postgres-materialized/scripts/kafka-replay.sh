#!/bin/bash

# Kafka Management Script: View topics or Reset consumer group offsets
# ./scripts/kafka-replay.sh -t user-events -r post-service-streams
# Use environment variable KAFKA_CONTAINER if set, otherwise default to "kafka"
: "${KAFKA_CONTAINER:="kafka"}"
BOOTSTRAP_SERVERS="localhost:9092"
TOPIC="user-events"
OFFSET="earliest"
PARTITION=""

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo "Options:"
    echo "  -t, --topic TOPIC         Topic name (default: user-events)"
    echo "  -s, --servers SERVERS     Bootstrap servers (default: localhost:9092)"
    echo "  -o, --offset OFFSET       Starting offset (default: earliest)"
    echo "  -r, --reset-group GROUP   !!! DANGEROUS !!! Reset offsets for a consumer group to the beginning of the topic."
    echo "  -p, --partition PARTITION Partition number (default: all)"
    echo "  -h, --help                Show this help message"
    exit 1
}

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
        -r|--reset-group)
            RESET_GROUP="$2"
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
done

echo "Kafka Management Script (via docker exec)"
echo "=========================================="
echo "Topic: $TOPIC"
echo "Bootstrap Servers: $BOOTSTRAP_SERVERS"

# Topic existence check
echo "Checking if topic '$TOPIC' exists..."
if ! docker exec "$KAFKA_CONTAINER" kafka-topics --bootstrap-server "$BOOTSTRAP_SERVERS" --list | grep -q "^${TOPIC}$"; then
    echo "Error: Topic '$TOPIC' does not exist"
    exit 1
fi 
echo "Topic '$TOPIC' exists."
echo 
 
if [[ -n "$RESET_GROUP" ]]; then
    # --- Replay/Reset Logic ---
    echo "Action: Resetting offsets for consumer group '$RESET_GROUP'"
    echo "=========================================================="
    echo "!!! WARNING !!!"
    echo "This will reset the offsets for consumer group '$RESET_GROUP' on topic '$TOPIC' to the beginning."
    echo "The application using this group ('$RESET_GROUP') will re-process ALL messages from the start."
    read -p "Are you sure you want to continue? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "Resetting offsets..."
        docker exec "$KAFKA_CONTAINER" kafka-consumer-groups \
          --bootstrap-server "$BOOTSTRAP_SERVERS" \
          --group "$RESET_GROUP" \
          --topic "$TOPIC" \
          --reset-offsets --to-earliest --execute
        echo
        echo "Reset complete. Please RESTART the application for group '$RESET_GROUP' to start reprocessing."
    else
        echo "Operation cancelled."
    fi
else
    # --- View Logic (Original Functionality) ---
    echo "Action: Viewing messages"
    echo "Offset: $OFFSET"
    echo "Partition: ${PARTITION:-all}"
    echo "=========================="
    echo "Starting consumer to view messages... Press Ctrl+C to stop."
    
    CONSUMER_CMD="kafka-console-consumer --bootstrap-server $BOOTSTRAP_SERVERS --topic $TOPIC --property print.key=true --property key.separator=': '"
    if [ "$OFFSET" == "earliest" ]; then
        CONSUMER_CMD="$CONSUMER_CMD --from-beginning"
    fi
    if [ -n "$PARTITION" ]; then
        CONSUMER_CMD="$CONSUMER_CMD --partition $PARTITION"
    fi
    docker exec -it "$KAFKA_CONTAINER" $CONSUMER_CMD
fi