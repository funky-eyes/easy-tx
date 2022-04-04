package icu.funkye.easy.tx.config;

import java.util.HashMap;
import java.util.Map;
import icu.funkye.easy.tx.entity.SagaBranchTransaction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 陈健斌
 */
public class SagaContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaContext.class);

    private static final ThreadLocal<Map<Integer, SagaBranchTransaction>> sagaBranchs = ThreadLocal.withInitial(
            HashMap::new);

    public static void clear() {
        sagaBranchs.remove();
    }

    public static SagaBranchTransaction getBranch(Integer hashCode) {
        return sagaBranchs.get().get(hashCode);
    }

    public static SagaBranchTransaction addBranch(Integer hashCode, SagaBranchTransaction branchTransaction) {
        return sagaBranchs.get().put(hashCode, branchTransaction);
    }

}
