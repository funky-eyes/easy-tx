package icu.funkye.easy.tx.proxy;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionFactory {

    private static volatile  ConcurrentHashMap<String,ConnectionProxy> concurrentHashMap=new ConcurrentHashMap<>();

    public static ConcurrentHashMap<String, ConnectionProxy> getConcurrentHashMap() {
        return concurrentHashMap;
    }

    public static void setConcurrentHashMap(ConcurrentHashMap<String, ConnectionProxy> concurrentHashMap) {
        ConnectionFactory.concurrentHashMap = concurrentHashMap;
    }
}
