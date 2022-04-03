package icu.funkye.easy.tx.config;

/**
 * @author 陈健斌
 */
public enum EasyTxMode {
    EASY("easy"), SAGA("saga");

    final String mode;

    EasyTxMode(String mode) {
        this.mode = mode;
    }

    public String mode() {
        return mode;
    }

}
