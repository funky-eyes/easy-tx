package icu.funkye.easy.tx.entity;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import icu.funkye.easy.tx.config.EasyTxMode;

public class SagaBranchTransaction {

    String xid;

    String branchId;

    String mode = EasyTxMode.SAGA.name();

    boolean status = Boolean.FALSE;

    String clientId;

    Integer retryInterval = 10000;

    String confirm;

    String cancel;

    Class<?> confirmBeanName;

    Class<?> cancelBeanName;

    Object[] args;

    Class<?>[] parameterTypes;

    Object result;

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

    public Class<?> getCancelBeanName() {
        return cancelBeanName;
    }

    public void setCancelBeanName(Class<?> cancelBeanName) {
        this.cancelBeanName = cancelBeanName;
    }

    public String getCancel() {
        return cancel;
    }

    public void setCancel(String cancel) {
        this.cancel = cancel;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SagaBranchTransaction that = (SagaBranchTransaction)o;
        return Objects.equals(xid, that.xid) && Objects.equals(mode, that.mode)
            && Objects.equals(clientId, that.clientId) && Objects.equals(retryInterval, that.retryInterval)
            && Objects.equals(confirm, that.confirm) && Objects.equals(cancel, that.cancel)
            && Objects.equals(confirmBeanName, that.confirmBeanName)
            && Objects.equals(cancelBeanName, that.cancelBeanName) && Arrays.equals(args, that.args)
            && Arrays.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        int result1 =
            Objects.hash(xid, mode, clientId, retryInterval, confirm, cancel, confirmBeanName, cancelBeanName);
        result1 = 31 * result1 + Arrays.hashCode(args);
        result1 = 31 * result1 + Arrays.hashCode(parameterTypes);
        return result1;
    }

}
