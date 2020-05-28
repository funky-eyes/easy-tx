package icu.funkye.easy.tx.proxy;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionFactory {

    private static volatile ConcurrentHashMap<String, List<ConnectionProxy>> concurrentHashMap =
        new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, List<ConnectionProxy>> getConcurrentHashMap() {
        return concurrentHashMap;
    }

    public static void setConcurrentHashMap(ConcurrentHashMap<String, List<ConnectionProxy>> concurrentHashMap) {
        ConnectionFactory.concurrentHashMap = concurrentHashMap;
    }
}
