package icu.funkye.easy.tx.entity;

import java.util.Date;

public class GlobalTransactionDO {

    String xid;

    String mode;

    Boolean status;

    Integer retryInterval = 10000;

    String method;

    Integer timeout;

    Boolean retry;

    Class<?> bean;

    Object[] args;

    Class<?>[] parameterTypes;

    Date createTime = new Date();

    Date modifyTime = new Date();

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Integer getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Class<?> getBean() {
        return bean;
    }

    public void setBean(Class<?> bean) {
        this.bean = bean;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Boolean getRetry() {
        return retry;
    }

    public void setRetry(Boolean retry) {
        this.retry = retry;
    }



}
