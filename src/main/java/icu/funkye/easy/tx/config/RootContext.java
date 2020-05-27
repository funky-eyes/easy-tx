package icu.funkye.easy.tx.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RootContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootContext.class);

    private static ContextCore CONTEXT_HOLDER = new ThreadLocalContextCore();

    /**
     * The constant KEY_XID.
     */
    public static final String KEY_XID = "TX_XID";

    /**
     * The constant KEY_XID.
     */
    public static final String XID_STATUS = "XID_STATUS";

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public static String getXID() {
        String xid = CONTEXT_HOLDER.get(KEY_XID);
        if (StringUtils.isNotBlank(xid)) {
            return xid;
        }
        return null;
    }

    /**
     * Bind.
     *
     * @param xid
     *            the xid
     */
    public static void bind(String xid) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("bind {}", xid);
        }
        CONTEXT_HOLDER.put(KEY_XID, xid);
    }

    /**
     * Unbind string.
     *
     * @return the string
     */
    public static String unbind() {
        String xid = CONTEXT_HOLDER.remove(KEY_XID);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("unbind {} ", xid);
        }
        return xid;
    }
}
