/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package icu.funkye.easy.tx.integration.dubbo;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import icu.funkye.easy.tx.config.RootContext;


/**
 * The type Transaction propagation filter.
 *
 * @author sharajava
 */
@Activate(group = {"provider", "consumer"}, order = 100)
public class ApacheDubboTransactionPropagationFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApacheDubboTransactionPropagationFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String xid = RootContext.getXID();

        String rpcXid = getRpcXid();
        String rpcRetry = getRpcXid();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in RootContext[{}] xid in RpcContext[{}]", xid, rpcXid);
        }
        boolean bind = false;
        if (xid != null) {
            RpcContext.getContext().setAttachment(RootContext.KEY_XID, xid);
            RpcContext.getContext().setAttachment(RootContext.TX_RETRY, RootContext.getRetry());
        } else {
            if (rpcXid != null) {
                RootContext.bind(rpcXid);
                if(rpcRetry!=null) {
                    RootContext.bind(rpcXid);
                }
                bind = true;
            }
        }
        try {
            return invoker.invoke(invocation);
        } finally {
            if (bind) {
                String unbindXid = RootContext.unbind();
                if (!rpcXid.equalsIgnoreCase(unbindXid)) {
                    if (unbindXid != null) {
                        RootContext.bind(unbindXid);
                        LOGGER.warn("bind xid [{}] back to RootContext", unbindXid);
                    }
                }
            }
        }
    }

    /**
     * get rpc xid
     * @return
     */
    private String getRpcXid() {
        String rpcXid = RpcContext.getContext().getAttachment(RootContext.KEY_XID);
        if (rpcXid == null) {
            rpcXid = RpcContext.getContext().getAttachment(RootContext.KEY_XID.toLowerCase());
        }
        return rpcXid;
    }

    /**
     * get rpc retry
     * @return
     */
    private String getRpcRetry() {
        String retry = RpcContext.getContext().getAttachment(RootContext.TX_RETRY);
        if (retry == null) {
            retry = RpcContext.getContext().getAttachment(RootContext.TX_RETRY.toLowerCase());
        }
        return retry;
    }

}
