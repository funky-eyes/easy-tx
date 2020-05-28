package icu.funkye.easy.tx.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈健斌 funkye
 */
@ConfigurationProperties(prefix = RocketMqProperties.ROCKET_PREFIX)
public class RocketMqProperties {
    public static final String ROCKET_PREFIX = EasyTxProperties.EASY_TX_PREFIX + ".rocket";

    private String nameServer;

    private String group;

    private String topic;

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
