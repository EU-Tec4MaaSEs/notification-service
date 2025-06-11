package gr.atc.t4m.enums;

import java.util.Arrays;

/*
 * Enum for Kafka Message Bus Topics - Finite Number of Topics
 */
public enum MessageBusTopic {
    SERVICE_DECOMPOSITION_FINISHED("service-decomposition-finished"),
    SUPPLY_CHAIN_NEGOTIATION("supply-chain-negotiation"),
    SUPPLY_CHAIN_ORDER_STATUS("supply-chain-order-status"),
    STAKEHOLDERS_MATCHED("stakeholders-matched"),
    DATASPACE_ORGANIZATION_ONBOARDING("dataspace-organization-onboarding"),
    SERVICE_COMPOSITION_FINISHED("service-composition-finished"),
    POST_OPTIMIZATION_FINISHED("post-optimization-finished");

    private final String topic;

    MessageBusTopic(final String topic) {
        this.topic = topic;
    }

    public static boolean isValidTopic(String topic) {
        if (topic == null) {
            return false;
        }

        return Arrays.stream(MessageBusTopic.values())
                .anyMatch(enumValue -> enumValue.topic.equals(topic));
    }

    @Override
    public String toString() {
        return topic;
    }
}
