package icu.funkye.easy.tx.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author 陈健斌
 */
@ConfigurationProperties(prefix = EasyTxProperties.EASY_TX_PREFIX)
public class EasyTxProperties {

    public static final String EASY_TX_PREFIX = "easy.tx";

    private boolean enable;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }
}
