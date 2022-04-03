package icu.funkye.easy.tx.properties;

import java.util.ArrayList;
import java.util.List;
import icu.funkye.easy.tx.config.EasyTxMode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author 陈健斌
 */
@ConfigurationProperties(prefix = EasyTxProperties.EASY_TX_PREFIX)
@Component
public class EasyTxProperties {

    public static final String EASY_TX_PREFIX = "easy.tx";

    private boolean enable;

    private String clientId;

    private String onlyUseMode = "easy,saga";

    private List<String> useModes = new ArrayList<>();

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getOnlyUseMode() {
        return onlyUseMode;
    }

    public void setOnlyUseMode(String onlyUseMode) {
        this.onlyUseMode = onlyUseMode;
        if (StringUtils.isNoneBlank(onlyUseMode)) {
            for (String mode : onlyUseMode.split(",")) {
                useModes.add(mode);
            }
        } else {
            for (EasyTxMode value : EasyTxMode.values()) {
                useModes.add(value.mode());
            }
        }
    }

    public List<String> getUseModes() {
        return useModes;
    }

    public void setUseModes(List<String> useModes) {
        this.useModes = useModes;
    }
}
