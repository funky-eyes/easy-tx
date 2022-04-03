package icu.funkye.easy.tx.config;

import icu.funkye.easy.tx.config.annotation.GlobalTransaction;
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
     * The constant TX_MODE.
     */
    public static final String TX_MODE = "TX_MODE";

    public static final String TX_RETRY = "TX_RETRY";

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

    public static String getRetry() {
        String retry = CONTEXT_HOLDER.get(TX_RETRY);
        if (StringUtils.isNotBlank(retry)) {
            return retry;
        }
        return "false";
    }

    /**
     * Bind.
     *
     * @param xid the xid
     */
    public static void bind(String xid) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("bind {}", xid);
        }
        CONTEXT_HOLDER.put(KEY_XID, xid);
    }

    /**
     * Bind.
     *
     * @param mode the mode
     */
    public static void bindMode(EasyTxMode mode) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("bind mode {}", mode);
        }
        CONTEXT_HOLDER.put(TX_MODE, mode.mode);
    }

    /**
     * Bind.
     *
     * @param retry the retry
     */
    public static void bindRetry(String retry) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("bind retry {}", retry);
        }
        CONTEXT_HOLDER.put(TX_RETRY, retry);
    }

    public static boolean isSaga() {
        return StringUtils.equalsIgnoreCase(CONTEXT_HOLDER.get(TX_MODE), EasyTxMode.SAGA.mode());
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
        CONTEXT_HOLDER.remove(TX_RETRY);
        CONTEXT_HOLDER.remove(TX_MODE);
        return xid;
    }
}
