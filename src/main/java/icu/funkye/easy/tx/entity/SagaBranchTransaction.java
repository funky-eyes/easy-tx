package icu.funkye.easy.tx.entity;

import java.util.Date;
import icu.funkye.easy.tx.config.EasyTxMode;

public class SagaBranchTransaction {

    String xid;

    String branchId;

    String mode = EasyTxMode.SAGA.name();

    boolean status = Boolean.FALSE;

    String clientId;

    Integer retryInterval = 10000;

    String confirm;

    Class<?> confirmBeanName;

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

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Integer getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Integer retryInterval) {
        this.retryInterval = retryInterval;
    }

    public String getConfirm() {
        return confirm;
    }

    public void setConfirm(String confirm) {
        this.confirm = confirm;
    }

    public Class<?> getConfirmBeanName() {
        return confirmBeanName;
    }

    public void setConfirmBeanName(Class<?> confirmBeanName) {
        this.confirmBeanName = confirmBeanName;
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

}
